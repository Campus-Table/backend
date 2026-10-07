package com.campustable.campus_table.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "orders",
        uniqueConstraints = @UniqueConstraint(columnNames = {"store_id", "arrival_date", "waiting_number"}))
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id")
    private Store store;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(nullable = false)
    private int totalPrice;

    private LocalDate arrivalDate;

    private Integer waitingNumber;

    @CreationTimestamp
    private LocalDateTime orderedAt;

    private LocalDateTime arrivedAt;

    private LocalDateTime expectedReadyAt;

    private LocalDateTime receivedAt;

    private LocalDateTime leaveAt;

    private LocalDateTime cancelledAt;

    public void arrive(LocalDateTime now, int waitingNumber, LocalDateTime expectedReadyAt) {
        this.status = OrderStatus.COOKING;
        this.arrivedAt = now;
        this.arrivalDate = now.toLocalDate();
        this.waitingNumber = waitingNumber;
        this.expectedReadyAt = expectedReadyAt;
    }

    public void markReady() {
        this.status = OrderStatus.READY;
    }

    public void receive(LocalDateTime receivedAt, LocalDateTime leaveAt) {
        this.status = OrderStatus.RECEIVED;
        this.receivedAt = receivedAt;
        this.leaveAt = leaveAt;
    }

    public void cancel(LocalDateTime now) {
        this.status = OrderStatus.CANCELLED;
        this.cancelledAt = now;
    }
}
