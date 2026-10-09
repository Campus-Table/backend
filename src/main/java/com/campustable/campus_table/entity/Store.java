package com.campustable.campus_table.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "stores")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cafeteria_id")
    private Cafeteria cafeteria;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 200)
    private String description;

    @Column(length = 30)
    private String category;

    @Column(nullable = false)
    @Builder.Default
    private int avgWaitMinutes = 2;

    @Column(length = 500)
    private String imageUrl;

    @Column(length = 200)
    private String imageObjectKey;

    public void changeImage(String url, String key) {
        this.imageUrl = url;
        this.imageObjectKey = key;
    }

    public void update(String name, String description, String category, int avgWaitMinutes) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.avgWaitMinutes = avgWaitMinutes;
    }

    @CreationTimestamp
    private LocalDateTime createdAt;
}
