package com.campustable.campus_table.service;

import com.campustable.campus_table.dto.AdminDtos.*;
import com.campustable.campus_table.dto.OrderDtos.ItemResponse;
import com.campustable.campus_table.entity.Order;
import com.campustable.campus_table.entity.OrderItem;
import com.campustable.campus_table.entity.OrderStatus;
import com.campustable.campus_table.repository.OrderItemRepository;
import com.campustable.campus_table.repository.OrderRepository;
import com.campustable.campus_table.repository.StoreRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ArrivalCodeService arrivalCodeService;
    private final OccupancyService occupancyService;
    private final StoreRepository storeRepository;

    public ArrivalCodeResponse arrivalCode(Long cafeteriaId) {
        LocalDate today = LocalDate.now();
        return new ArrivalCodeResponse(cafeteriaId, today, arrivalCodeService.getOrCreate(cafeteriaId, today).getCode());
    }

    @Transactional(readOnly = true)
    public DashboardResponse dashboard(Long cafeteriaId) {
        var o = occupancyService.occupancy(cafeteriaId); // 없는 식당이면 CAFETERIA_NOT_FOUND
        LocalDate today = LocalDate.now();
        long todayOrders = orderRepository.countOrdered(cafeteriaId, today.atStartOfDay(),
                today.plusDays(1).atStartOfDay(), OrderStatus.CANCELLED);
        return new DashboardResponse(o.currentPeople(), o.waitingPeople(), o.seatCount(), o.usageRate(),
                o.congestionLevel(), todayOrders);
    }

    /** 가게별 오늘 주문 수(취소 제외), 현재 조리 중인 주문 수, 인기 메뉴 TOP3 (주문 수 기준) */
    @Transactional(readOnly = true)
    public List<StoreStat> storeStats(Long cafeteriaId) {
        occupancyService.occupancy(cafeteriaId); // 존재 확인
        LocalDate today = LocalDate.now();
        LocalDateTime from = today.atStartOfDay(), to = today.plusDays(1).atStartOfDay();

        Map<Long, Long> ordered = toCountMap(
                orderRepository.countOrderedByStore(cafeteriaId, from, to, OrderStatus.CANCELLED));
        Map<Long, Long> waiting = toCountMap(
                orderRepository.countWaitingByStore(cafeteriaId, OrderStatus.COOKING, LocalDateTime.now()));

        Map<Long, List<TopMenu>> topByStore = new HashMap<>();
        orderItemRepository.menuOrderCountsByCafeteria(cafeteriaId, from, to, OrderStatus.CANCELLED).stream()
                .map(r -> Map.entry((Long) r[0], new TopMenu((Long) r[1], (String) r[2], (Long) r[3])))
                .collect(Collectors.groupingBy(entry -> entry.getKey(),
                        Collectors.mapping(entry -> entry.getValue(), Collectors.toList())))
                .forEach((storeId, menus) -> topByStore.put(storeId, menus.stream()
                        .sorted(Comparator.comparingLong((TopMenu menu) -> menu.count()).reversed()
                                .thenComparing(menu -> menu.menuId()))
                        .limit(3).toList()));

        return storeRepository.findByCafeteriaId(cafeteriaId).stream()
                .sorted(Comparator.comparing((com.campustable.campus_table.entity.Store store) -> store.getId()))
                .map(s -> new StoreStat(s.getId(), s.getName(), ordered.getOrDefault(s.getId(), 0L),
                        waiting.getOrDefault(s.getId(), 0L), topByStore.getOrDefault(s.getId(), List.of())))
                .toList();
    }

    /** 오늘 도착 인증했고 아직 수령하지 않은 주문 (조리 중 + 예상 시간이 지난 수령 대기) */
    @Transactional(readOnly = true)
    public List<WaitingOrder> waitings(Long cafeteriaId) {
        occupancyService.occupancy(cafeteriaId); // 존재 확인
        LocalDateTime now = LocalDateTime.now();
        List<Order> orders = orderRepository.findWaitings(cafeteriaId, now.toLocalDate(),
                List.of(OrderStatus.COOKING, OrderStatus.READY));
        Map<Long, List<OrderItem>> items = orders.isEmpty() ? Map.of()
                : orderItemRepository.findByOrderIdIn(orders.stream().map(order -> order.getId()).toList()).stream()
                .collect(Collectors.groupingBy(i -> i.getOrder().getId()));
        return orders.stream().map(o -> {
            boolean ready = o.getStatus() == OrderStatus.READY || !o.getExpectedReadyAt().isAfter(now);
            long remaining = ready ? 0 : Math.max(0, java.time.Duration.between(now, o.getExpectedReadyAt()).toSeconds());
            return new WaitingOrder(o.getId(), o.getWaitingNumber(), o.getStore().getId(), o.getStore().getName(),
                    ready ? OrderStatus.READY : OrderStatus.COOKING,
                    items.getOrDefault(o.getId(), List.of()).stream()
                            .map(i -> new WaitingItem(i.getMenu().getId(), i.getMenu().getName(), i.getQuantity()))
                            .toList(),
                    o.getArrivedAt(), o.getExpectedReadyAt(), remaining);
        }).toList();
    }

    private Map<Long, Long> toCountMap(List<Object[]> rows) {
        Map<Long, Long> map = new HashMap<>();
        rows.forEach(r -> map.put((Long) r[0], (Long) r[1]));
        return map;
    }

    /** 가게별 현재 대기번호 현황(조리 중인 주문) */
    @Transactional(readOnly = true)
    public List<StoreQueue> queues(Long cafeteriaId) {
        Map<Long, List<Order>> byStore = orderRepository
                .findCooking(cafeteriaId, OrderStatus.COOKING, LocalDateTime.now()).stream()
                .collect(Collectors.groupingBy(o -> o.getStore().getId(), LinkedHashMap::new, Collectors.toList()));
        return byStore.values().stream()
                .map(list -> new StoreQueue(list.get(0).getStore().getId(), list.get(0).getStore().getName(),
                        list.size(), list.stream().map(order -> order.getWaitingNumber()).toList()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminOrderResponse> orders(LocalDate date, Long storeId, OrderStatus status) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate day = date == null ? now.toLocalDate() : date;
        List<Order> orders = orderRepository.search(day.atStartOfDay(), day.plusDays(1).atStartOfDay(), storeId, status);
        Map<Long, List<OrderItem>> items = orders.isEmpty() ? Map.of()
                : orderItemRepository.findByOrderIdIn(orders.stream().map(order -> order.getId()).toList()).stream()
                .collect(Collectors.groupingBy(i -> i.getOrder().getId()));
        return orders.stream().map(o -> {
            // 조회 시각 기준 표시 상태(조리 완료 시각이 지났으면 READY)
            OrderStatus shown = o.getStatus() == OrderStatus.COOKING && !o.getExpectedReadyAt().isAfter(now)
                    ? OrderStatus.READY : o.getStatus();
            return new AdminOrderResponse(o.getId(), o.getUser().getId(), o.getUser().getStudentNumber(),
                    o.getUser().getName(), o.getStore().getId(), o.getStore().getName(), shown, o.getTotalPrice(),
                    o.getWaitingNumber(),
                    items.getOrDefault(o.getId(), List.of()).stream().map(ItemResponse::from).toList(),
                    o.getOrderedAt(), o.getArrivedAt(), o.getExpectedReadyAt(), o.getReceivedAt());
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<MenuOrderCount> menuCounts(LocalDate date, Long storeId) {
        LocalDate day = date == null ? LocalDate.now() : date;
        return orderItemRepository.menuCounts(day.atStartOfDay(), day.plusDays(1).atStartOfDay(),
                        storeId, OrderStatus.CANCELLED).stream()
                .map(r -> new MenuOrderCount((Long) r[0], (String) r[1], (Long) r[2], (String) r[3],
                        (Long) r[4], (Long) r[5]))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HourlyUsage> hourlyUsage(Long cafeteriaId, LocalDate date) {
        LocalDate day = date == null ? LocalDate.now() : date;
        LocalDateTime from = day.atStartOfDay(), to = day.plusDays(1).atStartOfDay();
        int[] ordered = new int[24], arrived = new int[24], received = new int[24];
        for (Order o : orderRepository.findActiveInRange(cafeteriaId, from, to)) {
            count(ordered, o.getOrderedAt(), from, to);
            count(arrived, o.getArrivedAt(), from, to);
            count(received, o.getReceivedAt(), from, to);
        }
        // 시간대별 이용 인원은 이용 구간(수령 ~ 이용 종료)의 겹침으로 계산 (인원 스냅샷 방식이 확정되면 스냅샷 우선으로 확장)
        var people = HourlyOccupancy.compute(
                orderRepository.findUsageIntervals(cafeteriaId, from, to).stream()
                        .map(o -> new HourlyOccupancy.Interval(o.getReceivedAt(), o.getLeaveAt())).toList(), day);
        return java.util.stream.IntStream.range(0, 24)
                .mapToObj(h -> new HourlyUsage(h, ordered[h], arrived[h], received[h],
                        people.get(h).peak(), people.get(h).avg())).toList();
    }

    private void count(int[] bucket, LocalDateTime t, LocalDateTime from, LocalDateTime to) {
        if (t != null && !t.isBefore(from) && t.isBefore(to)) {
            bucket[t.getHour()]++;
        }
    }
}
