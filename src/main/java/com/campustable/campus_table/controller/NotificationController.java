package com.campustable.campus_table.controller;

import com.campustable.campus_table.config.AuthUser;
import com.campustable.campus_table.dto.NotificationDtos.NotificationResponse;
import com.campustable.campus_table.dto.NotificationDtos.ReadAllResponse;
import com.campustable.campus_table.dto.NotificationDtos.UnreadCountResponse;
import com.campustable.campus_table.service.NotificationService;
import com.campustable.campus_table.service.OrderService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final OrderService orderService;

    /** 조회 전에 진행 중 주문의 시간 경과 상태를 반영해, 수령 안내 등이 이번 응답에 포함되게 한다. */
    @GetMapping
    public List<NotificationResponse> list(@AuthenticationPrincipal AuthUser user,
                                           @RequestParam(defaultValue = "false") boolean unreadOnly,
                                           @RequestParam(defaultValue = "30") int limit) {
        orderService.syncActiveOrders(user.userId());
        return notificationService.list(user.userId(), unreadOnly, limit);
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse unreadCount(@AuthenticationPrincipal AuthUser user) {
        orderService.syncActiveOrders(user.userId());
        return new UnreadCountResponse(notificationService.unreadCount(user.userId()));
    }

    @PatchMapping("/read-all")
    public ReadAllResponse readAll(@AuthenticationPrincipal AuthUser user) {
        return new ReadAllResponse(notificationService.readAll(user.userId()));
    }

    @PatchMapping("/{notificationId}/read")
    public NotificationResponse read(@AuthenticationPrincipal AuthUser user, @PathVariable Long notificationId) {
        return notificationService.markRead(user.userId(), notificationId);
    }
}
