package com.campustable.campus_table.dto;

import com.campustable.campus_table.common.CongestionLevel;
import com.campustable.campus_table.entity.Menu;
import com.campustable.campus_table.entity.OrderStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class AdminDtos {

    private AdminDtos() {
    }

    public record ArrivalCodeResponse(Long cafeteriaId, LocalDate date, String code) {
    }

    public record StoreQueue(Long storeId, String storeName, int waitingPeople, List<Integer> waitingNumbers) {
    }

    public record AdminOrderResponse(Long orderId, Long userId, String studentNumber, String userName,
                                     Long storeId, String storeName, OrderStatus status, int totalPrice,
                                     Integer waitingNumber, List<OrderDtos.ItemResponse> items,
                                     LocalDateTime orderedAt, LocalDateTime arrivedAt,
                                     LocalDateTime expectedReadyAt, LocalDateTime receivedAt) {
    }

    public record MenuOrderCount(Long menuId, String menuName, Long storeId, String storeName,
                                 long orderCount, long totalQuantity) {
    }

    public record HourlyUsage(int hour, int orderCount, int arrivalCount, int receivedCount,
                              int peakPeople, double avgPeople) {
    }

    /** 대시보드: 이용·대기 인원, 이용률, 혼잡도 + 오늘 주문 수(취소 제외) */
    public record DashboardResponse(int currentPeople, int waitingPeople, int seatCount, double usageRate,
                                    CongestionLevel congestionLevel, long todayOrders) {
    }

    public record TopMenu(Long menuId, String menuName, long count) {
    }

    public record StoreStat(Long storeId, String storeName, long todayOrders, long waitingCount,
                            List<TopMenu> topMenus) {
    }

    public record WaitingItem(Long menuId, String menuName, int quantity) {
    }

    public record WaitingOrder(Long orderId, int waitingNumber, Long storeId, String storeName,
                               OrderStatus status, List<WaitingItem> items, LocalDateTime arrivedAt,
                               LocalDateTime expectedReadyAt, long remainingSeconds) {
    }

    public record MenuRequest(
            @NotBlank(message = "메뉴 이름을 입력해주세요.") @Size(max = 100, message = "메뉴 이름은 100자 이하여야 합니다.") String name,
            @Min(value = 0, message = "가격은 0원 이상이어야 합니다.") int price,
            @Size(max = 500, message = "이미지 URL은 500자 이하여야 합니다.") String imageUrl) {
    }

    public record StoreRequest(
            @NotBlank @Size(max = 100) String name,
            @Size(max = 200) String description,
            @Size(max = 30) String category,
            @NotNull @Min(0) Integer avgWaitMinutes) { }

    public record StoreAdminResponse(Long id, Long cafeteriaId, String name, String description,
                                     String category, int avgWaitMinutes, String imageUrl) {
        public static StoreAdminResponse from(com.campustable.campus_table.entity.Store s) {
            return new StoreAdminResponse(s.getId(), s.getCafeteria().getId(), s.getName(),
                    s.getDescription(), s.getCategory(), s.getAvgWaitMinutes(), s.getImageUrl());
        }
    }

    public record AvailabilityRequest(@NotNull(message = "available 값이 필요합니다.") Boolean available) {
    }

    public record MenuAdminResponse(Long id, Long storeId, String name, int price, String imageUrl, boolean available) {
        public static MenuAdminResponse from(Menu m) {
            return new MenuAdminResponse(m.getId(), m.getStore().getId(), m.getName(), m.getPrice(),
                    m.getImageUrl(), m.isAvailable());
        }
    }
}
