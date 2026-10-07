package com.campustable.campus_table.dto;

import com.campustable.campus_table.entity.Order;
import com.campustable.campus_table.entity.OrderItem;
import com.campustable.campus_table.entity.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public final class OrderDtos {

    private OrderDtos() {
    }

    public record ItemRequest(
            @NotNull(message = "menuId가 필요합니다.") Long menuId,
            @Min(value = 1, message = "수량은 1개 이상이어야 합니다.")
            @Max(value = 20, message = "수량은 20개 이하여야 합니다.") int quantity) {
    }

    public record CreateRequest(
            @NotNull(message = "storeId가 필요합니다.") Long storeId,
            @NotEmpty(message = "주문할 메뉴를 선택해주세요.") List<@Valid ItemRequest> items) {
    }

    public record ArrivalRequest(@NotBlank(message = "현장 번호를 입력해주세요.") String code) {
    }

    public record ItemResponse(Long menuId, String menuName, int quantity, int unitPrice) {
        public static ItemResponse from(OrderItem i) {
            return new ItemResponse(i.getMenu().getId(), i.getMenu().getName(), i.getQuantity(), i.getUnitPrice());
        }
    }

    public record OrderResponse(Long orderId, Long storeId, String storeName, OrderStatus status, int totalPrice,
                                List<ItemResponse> items, Integer waitingNumber,
                                LocalDateTime expectedReadyAt, Long remainingSeconds,
                                LocalDateTime orderedAt, LocalDateTime arrivedAt, LocalDateTime receivedAt,
                                LocalDateTime leaveAt, LocalDateTime cancelledAt) {

        public static OrderResponse of(Order o, List<OrderItem> items, LocalDateTime now) {
            Long remaining = o.getStatus() == OrderStatus.COOKING && o.getExpectedReadyAt() != null
                    ? Math.max(0, Duration.between(now, o.getExpectedReadyAt()).toSeconds()) : null;
            return new OrderResponse(o.getId(), o.getStore().getId(), o.getStore().getName(), o.getStatus(),
                    o.getTotalPrice(), items.stream().map(ItemResponse::from).toList(), o.getWaitingNumber(),
                    o.getExpectedReadyAt(), remaining, o.getOrderedAt(), o.getArrivedAt(),
                    o.getReceivedAt(), o.getLeaveAt(), o.getCancelledAt());
        }
    }
}
