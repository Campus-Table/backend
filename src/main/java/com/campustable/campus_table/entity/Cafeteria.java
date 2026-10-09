package com.campustable.campus_table.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "cafeterias")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Cafeteria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private int seatCount;

    @Column(nullable = false)
    @Builder.Default
    private int diningMinutes = 30;

    private LocalTime openingTime;

    private LocalTime closingTime;

    @CreationTimestamp
    private LocalDateTime createdAt;

    /** null이 아닌 값만 변경한다. */
    public void update(String name, Integer seatCount, Integer diningMinutes,
                       LocalTime openingTime, LocalTime closingTime) {
        if (name != null) this.name = name;
        if (seatCount != null) this.seatCount = seatCount;
        if (diningMinutes != null) this.diningMinutes = diningMinutes;
        if (openingTime != null) this.openingTime = openingTime;
        if (closingTime != null) this.closingTime = closingTime;
    }
}
