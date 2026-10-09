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
        LocalDateTime now = LocalDateTime.now(java.time.ZoneId.of("Asia/Seoul"));
        User user = lockUser(userId); // 사용자별 직렬화(중복 주문/잔액 경합 방지)
        Store store = storeRepository.findById(req.storeId())
                .orElseThrow(() -> new CustomException(ErrorCode.STORE_NOT_FOUND));

        orderRepository.findByUserIdAndStatusIn(userId, ACTIVE).forEach(o -> sync(o, now));
        if (!orderRepository.findByUserIdAndStatusIn(userId, ACTIVE).isEmpty()) {
            throw new CustomException(ErrorCode.ACTIVE_ORDER_EXISTS);
        }

        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (ItemRequest item : req.items()) {
            quantities.merge(item.menuId(), item.quantity(), (a, b) -> a + b);
        }
        Map<Long, Menu> menus = menuRepository.findAllById(quantities.keySet()).stream()
                .collect(Collectors.toMap(menu -> menu.getId(), m -> m));
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
        LocalDateTime now = LocalDateTime.now(java.time.ZoneId.of("Asia/Seoul"));
        List<Order> orders = orderRepository.findByUserIdOrderByIdDesc(userId);
        orders.forEach(o -> sync(o, now));
        return toResponses(orders, now);
    }

    /** 진행 중(PAID/COOKING/READY)인 내 주문 1건. 없으면 empty. */
    @Transactional
    public Optional<OrderResponse> current(Long userId) {
        LocalDateTime now = LocalDateTime.now(java.time.ZoneId.of("Asia/Seoul"));
        return syncAndFindActive(userId, now).stream()
                .max(Comparator.comparing((Order order) -> order.getId()))
                .map(o -> toResponses(List.of(o), now).get(0));
    }

    /** 시간 경과에 따른 상태 전이(수령 안내/자동 취소 알림 포함)만 반영한다. 알림 조회 전에 호출된다. */
    @Transactional
    public void syncActiveOrders(Long userId) {
        syncAndFindActive(userId, LocalDateTime.now(java.time.ZoneId.of("Asia/Seoul")));
    }

    @Transactional
    public OrderResponse get(Long userId, Long orderId) {
        LocalDateTime now = LocalDateTime.now(java.time.ZoneId.of("Asia/Seoul"));
        Order order = findOwned(userId, orderId, false);
        sync(order, now);
        return toResponses(List.of(order), now).get(0);
    }

    // READ_COMMITTED: 가게 락을 기다리는 동안 앞선 요청이 확정한 대기번호를 max 조회가 볼 수 있어야 한다.
    // (기본 REPEATABLE READ는 락 대기 전에 잡힌 스냅샷을 써서 번호가 중복 발급된다)
    @Transactional(isolation = Isolation.READ_COMMITTED, noRollbackFor = CustomException.class)
    public OrderResponse arrive(Long userId, Long orderId, String code) {
        LocalDateTime now = LocalDateTime.now(java.time.ZoneId.of("Asia/Seoul"));
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

    @Transactional(noRollbackFor = CustomException.class)
    public OrderResponse cancel(Long userId, Long orderId) {
        LocalDateTime now = LocalDateTime.now(java.time.ZoneId.of("Asia/Seoul"));
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
        return switch (order.getStatus()) {
            case PAID -> !order.getOrderedAt().plusHours(1).isAfter(now);
            case COOKING -> !order.getExpectedReadyAt().isAfter(now);
            case READY -> true; // 이전 방식으로 남은 주문 정리
            default -> false;
        };
    }

    /**
     * 시간 경과에 따른 상태 전이를 조회 시점에 반영한다. 미인증 만료는 매분 작업에서도 처리한다.
     * PAID(1시간 미인증) -> CANCELLED + 환불, COOKING -> RECEIVED(예상 시간 경과 = 음식이 나온 것으로 보고
     * 수령 안내 알림 + 이용 시작; 학식당은 별도 수령 확인이 없음. 이전 방식의 READY 주문도 RECEIVED로 정리).
     * 상태를 바꿔야 할 때만 행을 잠그고 최신 상태를 다시 읽는다.
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
        if (order.getStatus() == OrderStatus.PAID) {
            cancelWithRefund(order, now, true);
            return;
        }
        LocalDateTime at = order.getExpectedReadyAt();
        if (order.getStatus() == OrderStatus.COOKING) {
            notificationService.foodReady(order);
        }
        order.receive(at, at.plusMinutes(diningMinutes(order)));
    }

    private void cancelWithRefund(Order order, LocalDateTime now, boolean auto) {
        order.cancel(now);
        int refund = order.getTotalPrice();
        if (refund > 0) {
            mileageService.apply(order.getUser().getId(), MileageType.REFUND, refund, order);
        }
        notificationService.cancelled(order, refund, auto, now);
    }

    @Transactional
    public void expireOrder(Long orderId, LocalDateTime now) {
        var found = orderRepository.findById(orderId).orElse(null);
        if (found == null) return;
        lockUser(found.getUser().getId());
        var order = orderRepository.findByIdForUpdate(orderId).orElse(null);
        if (order != null && order.getStatus() == OrderStatus.PAID) sync(order, now);
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
                .findByOrderIdIn(orders.stream().map(order -> order.getId()).toList()).stream()
                .collect(Collectors.groupingBy(i -> i.getOrder().getId()));
        return orders.stream()
                .map(o -> OrderResponse.of(o, items.getOrDefault(o.getId(), List.of()), now)).toList();
    }
}
