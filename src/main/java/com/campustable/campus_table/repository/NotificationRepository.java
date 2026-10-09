package com.campustable.campus_table.repository;

import com.campustable.campus_table.entity.Notification;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);

    List<Notification> findByUserIdAndReadFalseOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);

    long countByUserIdAndReadFalse(Long userId);

    @Modifying
    @Query("update Notification n set n.read = true where n.user.id = :userId and n.read = false")
    int markAllRead(@Param("userId") Long userId);

    /**
     * (order_id, type) 유니크를 이용한 멱등 저장.
     * 조회 시점에 만들어지는 알림(수령 안내, 자동 취소)은 동시 폴링으로 중복 생성될 수 있어 DB가 중복을 걸러낸다.
     */
    @Modifying
    @Query(value = "insert into notifications (user_id, order_id, type, title, message, is_read, created_at) "
            + "values (:userId, :orderId, :type, :title, :message, false, :createdAt) "
            + "on duplicate key update id = id", nativeQuery = true)
    void insertIfAbsent(@Param("userId") Long userId, @Param("orderId") Long orderId, @Param("type") String type,
                        @Param("title") String title, @Param("message") String message,
                        @Param("createdAt") LocalDateTime createdAt);
}
