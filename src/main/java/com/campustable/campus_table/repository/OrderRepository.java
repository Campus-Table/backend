package com.campustable.campus_table.repository;

import com.campustable.campus_table.entity.Order;
import com.campustable.campus_table.entity.OrderStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);

    List<Order> findByUserIdOrderByIdDesc(Long userId);

    List<Order> findByUserIdAndStatusIn(Long userId, Collection<OrderStatus> statuses);

    @Query("select coalesce(max(o.waitingNumber), 0) from Order o "
            + "where o.store.id = :storeId and o.arrivalDate = :date")
    int maxWaitingNumber(@Param("storeId") Long storeId, @Param("date") LocalDate date);

    /** 아직 조리 중인(예상 완료 시각이 지나지 않은) 같은 가게의 주문 수 */
    @Query("select count(o) from Order o where o.store.id = :storeId and o.arrivalDate = :date "
            + "and o.status = :status and o.expectedReadyAt > :now")
    long countCooking(@Param("storeId") Long storeId, @Param("date") LocalDate date,
                      @Param("status") OrderStatus status, @Param("now") LocalDateTime now);

    /** 학식당 현재 이용 인원: 수령했고 아직 이용 종료 시각 전인 주문 수 */
    @Query("select count(o) from Order o where o.store.cafeteria.id = :cafeteriaId "
            + "and o.status = :status and o.leaveAt > :now")
    long countInUse(@Param("cafeteriaId") Long cafeteriaId, @Param("status") OrderStatus status,
                    @Param("now") LocalDateTime now);

    /** 학식당 현재 대기 인원: 조리 중인 주문 수 */
    @Query("select count(o) from Order o where o.store.cafeteria.id = :cafeteriaId "
            + "and o.status = :status and o.expectedReadyAt > :now")
    long countWaiting(@Param("cafeteriaId") Long cafeteriaId, @Param("status") OrderStatus status,
                      @Param("now") LocalDateTime now);
}
