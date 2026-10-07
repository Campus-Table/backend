package com.campustable.campus_table.service;

import com.campustable.campus_table.dto.AdminDtos.*;
import com.campustable.campus_table.dto.OrderDtos.ItemResponse;
import com.campustable.campus_table.entity.Order;
import com.campustable.campus_table.entity.OrderItem;
import com.campustable.campus_table.entity.OrderStatus;
import com.campustable.campus_table.repository.OrderItemRepository;
import com.campustable.campus_table.repository.OrderRepository;
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

    public ArrivalCodeResponse arrivalCode(Long cafeteriaId) {
        LocalDate today = LocalDate.now();
        return new ArrivalCodeResponse(cafeteriaId, today, arrivalCodeService.getOrCreate(cafeteriaId, today).getCode());
    }

    public OccupancyService.Occupancy dashboard(Long cafeteriaId) {
        return occupancyService.occupancy(cafeteriaId);
    }

    /** 가게별 현재 대기번호 현황(조리 중인 주문) */
    @Transactional(readOnly = true)
    public List<StoreQueue> queues(Long cafeteriaId) {
        Map<Long, List<Order>> byStore = orderRepository
                .findCooking(cafeteriaId, OrderStatus.COOKING, LocalDateTime.now()).stream()
                .collect(Collectors.groupingBy(o -> o.getStore().getId(), LinkedHashMap::new, Collectors.toList()));
        return byStore.values().stream()
                .map(list -> new StoreQueue(list.get(0).getStore().getId(), list.get(0).getStore().getName(),
                        list.size(), list.stream().map(Order::getWaitingNumber).toList()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminOrderResponse> orders(LocalDate date, Long storeId, OrderStatus status) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate day = date == null ? now.toLocalDate() : date;
        List<Order> orders = orderRepository.search(day.atStartOfDay(), day.plusDays(1).atStartOfDay(), storeId, status);
        Map<Long, List<OrderItem>> items = orders.isEmpty() ? Map.of()
                : orderItemRepository.findByOrderIdIn(orders.stream().map(Order::getId).toList()).stream()
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
        return java.util.stream.IntStream.range(0, 24)
                .mapToObj(h -> new HourlyUsage(h, ordered[h], arrived[h], received[h])).toList();
    }

    private void count(int[] bucket, LocalDateTime t, LocalDateTime from, LocalDateTime to) {
        if (t != null && !t.isBefore(from) && t.isBefore(to)) {
            bucket[t.getHour()]++;
        }
    }
}
