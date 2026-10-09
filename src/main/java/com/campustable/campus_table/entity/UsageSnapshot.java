package com.campustable.campus_table.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "usage_snapshots")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UsageSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cafeteria_id")
    private Cafeteria cafeteria;

    @Column(nullable = false)
    private LocalDateTime recordedAt;

    @Column(nullable = false)
    private int currentPeople;

    @Column(nullable = false)
    private int waitingPeople;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(20) default 'MANUAL'")
    @Builder.Default
    private OccupancySource source = OccupancySource.MANUAL;
}
