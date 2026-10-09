package com.campustable.campus_table.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "mileage_charges")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MileageCharge {
    @Id private String id;
    @Column(nullable = false) private Long userId;
    @Column(nullable = false) private int amount;
    @Column(nullable = false) private LocalDateTime createdAt;
    @Column(unique = true) private String paymentKey;
    private Integer balanceAfter;
    public MileageCharge(String id, Long userId, int amount) {
        this.id = id; this.userId = userId; this.amount = amount;
        this.createdAt = LocalDateTime.now(java.time.ZoneId.of("Asia/Seoul"));
    }
    public void complete(String paymentKey, int balance) { this.paymentKey = paymentKey; this.balanceAfter = balance; }
}
