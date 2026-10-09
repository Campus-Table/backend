package com.campustable.campus_table.service;

import com.campustable.campus_table.common.CustomException;
import com.campustable.campus_table.common.ErrorCode;
import com.campustable.campus_table.dto.OrderDtos.CreateRequest;
import com.campustable.campus_table.dto.OrderDtos.ItemRequest;
import com.campustable.campus_table.dto.OrderDtos.OrderResponse;
import com.campustable.campus_table.entity.*;
import com.campustable.campus_table.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 잠금 순서 규칙: 항상 사용자 행 → 주문 행 순으로 잠근다 (환불이 사용자 행을 잠그므로 교착 방지).
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final List<OrderStatus> ACTIVE = List.of(OrderStatus.PAID, OrderStatus.COOKING, OrderStatus.READY);
    /** 도착 인증 전 취소(자동 취소 포함) 시 환불 비율(%) */
    private static final int REFUND_PERCENT = 50;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final StoreRepository storeRepository;
    private final MenuRepository menuRepository;
    private final UserRepository userRepository;
    private final MileageService mileageService;
    private final ArrivalCodeService arrivalCodeService;
    private final NotificationService notificationService;
    private final EntityManager em;

    @Transactional
    public OrderResponse create(Long userId, CreateRequest req) {
        LocalDateTime now = LocalDateTime.now();
        User user = lockUser(userId); // 사용자별 직렬화(중복 주문/잔액 경합 방지)
        Store store = storeRepository.findById(req.storeId())
                .orElseThrow(() -> new CustomException(ErrorCode.STORE_NOT_FOUND));

        orderRepository.findByUserIdAndStatusIn(userId, ACTIVE).forEach(o -> sync(o, now));
        if (!orderRepository.findByUserIdAndStatusIn(userId, ACTIVE).isEmpty()) {
            throw new CustomException(ErrorCode.ACTIVE_ORDER_EXISTS);
        }

        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (ItemRequest item : req.items()) {
            quantities.merge(item.menuId(), item.quantity(), Integer::sum);
        }
        Map<Long, Menu> menus = menuRepository.findAllById(quantities.keySet()).stream()
                .collect(Collectors.toMap(Menu::getId, m -> m));
        int total = 0;
        for (var e : quantities.entrySet()) {
            Menu menu = menus.get(e.getKey());
            if (menu == null || !menu.getStore().getId().equals(store.getId())) {
                throw new CustomException(ErrorCode.MENU_NOT_FOUND);
            }
            if (!menu.isAvailable()) {
                throw new CustomException(ErrorCode.MENU_UNAVAILABLE);
            }
            total += menu.getPrice() * e.getValue();
        }

        Order order = orderRepository.save(Order.builder()
                .user(user).store(store).status(OrderStatus.PAID).totalPrice(total).build());
        List<OrderItem> items = orderItemRepository.saveAll(quantities.entrySet().stream()
                .map(e -> OrderItem.builder().order(order).menu(menus.get(e.getKey()))
                        .quantity(e.getValue()).unitPrice(menus.get(e.getKey()).getPrice()).build())
                .toList());
        mileageService.apply(userId, MileageType.USE, total, order); // 잔액 부족 시 전체 롤백
        notificationService.orderPaid(order, items);
        return OrderResponse.of(order, items, now);
    }

    @Transactional
    public List<OrderResponse> myOrders(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        List<Order> orders = orderRepository.findByUserIdOrderByIdDesc(userId);
        orders.forEach(o -> sync(o, now));
        return toResponses(orders, now);
    }

    /** 진행 중(PAID/COOKING/READY)인 내 주문 1건. 없으면 empty. */
    @Transactional
    public Optional<OrderResponse> current(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        return syncAndFindActive(userId, now).stream()
                .max(Comparator.comparing(Order::getId))
                .map(o -> toResponses(List.of(o), now).get(0));
    }

    /** 시간 경과에 따른 상태 전이(수령 안내/자동 취소 알림 포함)만 반영한다. 알림 조회 전에 호출된다. */
    @Transactional
    public void syncActiveOrders(Long userId) {
        syncAndFindActive(userId, LocalDateTime.now());
    }

    @Transactional
    public OrderResponse get(Long userId, Long orderId) {
        LocalDateTime now = LocalDateTime.now();
        Order order = findOwned(userId, orderId, false);
        sync(order, now);
        return toResponses(List.of(order), now).get(0);
    }

    // READ_COMMITTED: 가게 락을 기다리는 동안 앞선 요청이 확정한 대기번호를 max 조회가 볼 수 있어야 한다.
    // (기본 REPEATABLE READ는 락 대기 전에 잡힌 스냅샷을 써서 번호가 중복 발급된다)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public OrderResponse arrive(Long userId, Long orderId, String code) {
        LocalDateTime now = LocalDateTime.now();
        lockUser(userId);
        Order order = findOwned(userId, orderId, true);
        sync(order, now);
        if (order.getStatus() != OrderStatus.PAID) {
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }
        Store store = order.getStore();
        String today = arrivalCodeService.getOrCreate(store.getCafeteria().getId(), now.toLocalDate()).getCode();
        if (!today.equals(code.trim())) {
            throw new CustomException(ErrorCode.INVALID_ARRIVAL_CODE);
        }
        em.find(Store.class, store.getId(), LockModeType.PESSIMISTIC_WRITE); // 가게별 대기번호 직렬화
        int number = orderRepository.maxWaitingNumber(store.getId(), now.toLocalDate()) + 1;
        long ahead = orderRepository.countCooking(store.getId(), now.toLocalDate(), OrderStatus.COOKING, now);
        // 예상 대기시간 = (앞 대기 인원 + 1) × 가게 평균 대기시간
        long expectedMinutes = (ahead + 1) * store.getAvgWaitMinutes();
        order.arrive(now, number, now.plusMinutes(expectedMinutes));
        notificationService.arrived(order, expectedMinutes);
        return toResponses(List.of(order), now).get(0);
    }

    @Transactional
    public OrderResponse receive(Long userId, Long orderId) {
        LocalDateTime now = LocalDateTime.now();
        lockUser(userId);
        Order order = findOwned(userId, orderId, true);
        sync(order, now);
        if (order.getStatus() != OrderStatus.READY) {
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }
        order.receive(now, now.plusMinutes(diningMinutes(order)));
        return toResponses(List.of(order), now).get(0);
    }

    @Transactional
    public OrderResponse cancel(Long userId, Long orderId) {
        LocalDateTime now = LocalDateTime.now();
        lockUser(userId);
        Order order = findOwned(userId, orderId, true);
        sync(order, now);
        if (order.getStatus() != OrderStatus.PAID) { // 도착 인증 전까지만 취소 가능
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }
        cancelWithRefund(order, now, false);
        return toResponses(List.of(order), now).get(0);
    }

    private List<Order> syncAndFindActive(Long userId, LocalDateTime now) {
        orderRepository.findByUserIdAndStatusIn(userId, ACTIVE).forEach(o -> sync(o, now));
        return orderRepository.findByUserIdAndStatusIn(userId, ACTIVE);
    }

    private static boolean needsSync(Order order, LocalDateTime now) {
        var today = now.toLocalDate();
        return switch (order.getStatus()) {
            case PAID -> order.getOrderedAt().toLocalDate().isBefore(today);
            case COOKING -> !order.getExpectedReadyAt().isAfter(now);
            case READY -> order.getArrivalDate().isBefore(today);
            default -> false;
        };
    }

    /**
     * 시간 경과에 따른 상태 전이를 조회 시점에 반영한다(스케줄러 없음).
     * PAID(전날 미인증) -> CANCELLED + 환불, COOKING -> READY(예상 시간 경과, 수령 안내 알림),
     * 전날 COOKING/READY -> RECEIVED(수령으로 간주).
     * 상태를 바꿔야 할 때만 행을 잠그고 최신 상태를 다시 읽는다 (폴링 요청이 수령 처리 결과를 덮어쓰는 것 방지).
     * ponytail: 전날 미수령 주문을 수령 처리하는 단순 규칙. 정책이 정해지면 변경.
     */
    private void sync(Order order, LocalDateTime now) {
        if (!needsSync(order, now)) {
            return;
        }
        if (order.getStatus() == OrderStatus.PAID) {
            lockUser(order.getUser().getId()); // 환불이 사용자 행을 잠그므로 사용자 → 주문 순서 유지
        }
        em.refresh(order, LockModeType.PESSIMISTIC_WRITE);
        if (!needsSync(order, now)) { // 잠금을 기다리는 사이 다른 요청이 이미 처리했을 수 있음
            return;
        }
        var today = now.toLocalDate();
        if (order.getStatus() == OrderStatus.PAID && order.getOrderedAt().toLocalDate().isBefore(today)) {
            cancelWithRefund(order, now, true);
            return;
        }
        if (order.getStatus() == OrderStatus.COOKING && !order.getExpectedReadyAt().isAfter(now)) {
            order.markReady();
            notificationService.foodReady(order);
        }
        if (order.getStatus() == OrderStatus.READY && order.getArrivalDate().isBefore(today)) {
            LocalDateTime at = order.getExpectedReadyAt();
            order.receive(at, at.plusMinutes(diningMinutes(order)));
        }
    }

    private void cancelWithRefund(Order order, LocalDateTime now, boolean auto) {
        order.cancel(now);
        int refund = order.getTotalPrice() * REFUND_PERCENT / 100;
        if (refund > 0) {
            mileageService.apply(order.getUser().getId(), MileageType.REFUND, refund, order);
        }
        notificationService.cancelled(order, refund, auto, now);
    }

    private User lockUser(Long userId) {
        return userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private int diningMinutes(Order order) {
        return order.getStore().getCafeteria().getDiningMinutes();
    }

    private Order findOwned(Long userId, Long orderId, boolean lock) {
        Order order = (lock ? orderRepository.findByIdForUpdate(orderId) : orderRepository.findById(orderId))
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
        if (!order.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        return order;
    }

    private List<OrderResponse> toResponses(List<Order> orders, LocalDateTime now) {
        if (orders.isEmpty()) {
            return List.of();
        }
        Map<Long, List<OrderItem>> items = orderItemRepository
                .findByOrderIdIn(orders.stream().map(Order::getId).toList()).stream()
                .collect(Collectors.groupingBy(i -> i.getOrder().getId()));
        return orders.stream()
                .map(o -> OrderResponse.of(o, items.getOrDefault(o.getId(), List.of()), now)).toList();
    }
}
