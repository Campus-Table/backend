package com.campustable.campus_table.service;

import com.campustable.campus_table.common.CustomException;
import com.campustable.campus_table.common.ErrorCode;
import com.campustable.campus_table.dto.NotificationDtos.NotificationResponse;
import com.campustable.campus_table.entity.*;
import com.campustable.campus_table.repository.NotificationRepository;
import com.campustable.campus_table.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 알림 생성(주문·충전 이벤트)과 조회. 주문 관련 알림은 (order_id, type)당 한 번만 만들어진다. */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final int MAX_LIMIT = 100;

    private final NotificationRepository repository;
    private final UserRepository userRepository;

    // ---------- 생성 ----------

    @Transactional
    public void orderPaid(Order order, List<OrderItem> items) {
        insert(order, NotificationType.ORDER_PAID, "선주문이 완료되었습니다",
                "%s %s 결제가 완료됐어요.".formatted(order.getStore().getName(), summary(items)),
                LocalDateTime.now());
    }

    /** 도착 인증: 대기번호 발급 → 조리 시작 순으로 저장해, 최신순 목록에서 조리 시작이 위에 온다. */
    @Transactional
    public void arrived(Order order, long expectedMinutes) {
        LocalDateTime at = order.getArrivedAt();
        insert(order, NotificationType.WAITING_NUMBER_ISSUED,
                "대기번호 %d번이 발급되었습니다".formatted(order.getWaitingNumber()),
                "도착 인증이 완료되어 대기번호가 발급됐어요.", at);
        insert(order, NotificationType.COOKING_STARTED, "음식 조리가 시작되었습니다",
                "예상 준비시간은 약 %d분입니다.".formatted(expectedMinutes), at);
    }

    /** 예상 준비 시각이 지났을 때 조회 시점에 호출된다. 시각은 예상 준비 시각으로 기록한다. */
    @Transactional
    public void foodReady(Order order) {
        insert(order, NotificationType.FOOD_READY, "음식을 수령하러 와주세요",
                "예상 준비시간이 지났습니다. %s 수령대로 와주세요.".formatted(order.getStore().getName()),
                order.getExpectedReadyAt());
    }

    @Transactional
    public void cancelled(Order order, int refund, boolean auto, LocalDateTime at) {
        String refundText = refund > 0
                ? "결제 금액의 50%%인 %,dP가 환불되었어요.".formatted(refund) : "환불 금액이 없습니다.";
        insert(order, NotificationType.ORDER_CANCELLED, "주문이 취소되었습니다",
                (auto ? "도착 인증을 하지 않아 주문이 자동 취소되었어요. " : "") + refundText, at);
    }

    @Transactional
    public void mileageCharged(Long userId, int amount) {
        User user = userRepository.getReferenceById(userId);
        repository.save(Notification.builder().user(user).type(NotificationType.MILEAGE_CHARGED)
                .title("마일리지가 충전되었습니다").message("%,dP가 충전됐어요.".formatted(amount))
                .createdAt(LocalDateTime.now()).build());
    }

    private void insert(Order order, NotificationType type, String title, String message, LocalDateTime at) {
        repository.insertIfAbsent(order.getUser().getId(), order.getId(), type.name(), title, message, at);
    }

    private String summary(List<OrderItem> items) {
        if (items.isEmpty()) {
            return "";
        }
        String first = items.get(0).getMenu().getName();
        return items.size() > 1 ? first + " 외 " + (items.size() - 1) + "개" : first;
    }

    // ---------- 조회 ----------

    @Transactional(readOnly = true)
    public List<NotificationResponse> list(Long userId, boolean unreadOnly, int limit) {
        var page = PageRequest.of(0, Math.min(Math.max(limit, 1), MAX_LIMIT));
        var found = unreadOnly
                ? repository.findByUserIdAndReadFalseOrderByCreatedAtDescIdDesc(userId, page)
                : repository.findByUserIdOrderByCreatedAtDescIdDesc(userId, page);
        return found.stream().map(NotificationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return repository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public NotificationResponse markRead(Long userId, Long notificationId) {
        Notification n = repository.findById(notificationId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND));
        if (!n.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        n.markRead();
        return NotificationResponse.from(n);
    }

    @Transactional
    public int readAll(Long userId) {
        return repository.markAllRead(userId);
    }
}
