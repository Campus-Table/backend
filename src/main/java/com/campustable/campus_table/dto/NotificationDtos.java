package com.campustable.campus_table.dto;

import com.campustable.campus_table.entity.Notification;
import com.campustable.campus_table.entity.NotificationType;
import java.time.LocalDateTime;

public final class NotificationDtos {

    private NotificationDtos() {
    }

    public record NotificationResponse(Long id, NotificationType type, String title, String message,
                                       Long orderId, boolean read, LocalDateTime createdAt) {
        public static NotificationResponse from(Notification n) {
            return new NotificationResponse(n.getId(), n.getType(), n.getTitle(), n.getMessage(),
                    n.getOrder() == null ? null : n.getOrder().getId(), n.isRead(), n.getCreatedAt());
        }
    }

    public record UnreadCountResponse(long count) {
    }

    public record ReadAllResponse(int updated) {
    }
}
