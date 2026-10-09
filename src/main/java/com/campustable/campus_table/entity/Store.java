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

    /**
     * null이 아닌 값만 변경한다 (부분 수정). 설명/분류는 빈 문자열을 보내면 값이 지워진다(null).
     * 이미지는 업로드 파일 정리가 필요해서 여기서 다루지 않는다 ({@link #changeImage} + ImageStorageService).
     */
    public void patch(String name, String description, String category, Integer avgWaitMinutes) {
        if (name != null) this.name = name;
        if (description != null) this.description = blankToNull(description);
        if (category != null) this.category = blankToNull(category);
        if (avgWaitMinutes != null) this.avgWaitMinutes = avgWaitMinutes;
    }

    private static String blankToNull(String value) {
        return value.isBlank() ? null : value.trim();
    }
}
