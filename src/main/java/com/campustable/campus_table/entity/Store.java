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

    @CreationTimestamp
    private LocalDateTime createdAt;

    /** null이 아닌 값만 변경한다. 설명/분류/이미지는 빈 문자열을 보내면 값이 지워진다(null). */
    public void update(String name, String description, String category, Integer avgWaitMinutes, String imageUrl) {
        if (name != null) this.name = name;
        if (description != null) this.description = blankToNull(description);
        if (category != null) this.category = blankToNull(category);
        if (avgWaitMinutes != null) this.avgWaitMinutes = avgWaitMinutes;
        if (imageUrl != null) this.imageUrl = blankToNull(imageUrl);
    }

    private static String blankToNull(String value) {
        return value.isBlank() ? null : value.trim();
    }
}
