package com.campustable.campus_table.dto;

import com.campustable.campus_table.entity.MileageTransaction;
import com.campustable.campus_table.entity.MileageType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public final class MileageDtos {

    private MileageDtos() {
    }

    public record ChargeConfirmRequest(
            @NotBlank(message = "paymentKey가 필요합니다.") String paymentKey,
            @NotBlank(message = "orderId가 필요합니다.") String orderId,
            @Min(value = 1000, message = "충전 금액은 1,000원 이상이어야 합니다.")
            @Max(value = 100000, message = "충전 금액은 100,000원 이하여야 합니다.") int amount) {
    }

    public record BalanceResponse(int balance) {
    }

    public record TransactionResponse(Long id, MileageType type, int amount, int balanceAfter,
                                      Long orderId, LocalDateTime createdAt) {
        public static TransactionResponse from(MileageTransaction t) {
            return new TransactionResponse(t.getId(), t.getType(), t.getAmount(), t.getBalanceAfter(),
                    t.getOrder() == null ? null : t.getOrder().getId(), t.getCreatedAt());
        }
    }
}
