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

    @Query("select o from Order o where o.orderedAt >= :from and o.orderedAt < :to "
            + "and (:storeId is null or o.store.id = :storeId) and (:status is null or o.status = :status) "
            + "order by o.id desc")
    List<Order> search(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                       @Param("storeId") Long storeId, @Param("status") OrderStatus status);

    /** 시간대별 통계용: 해당 기간에 주문/도착/수령 중 하나라도 발생한 주문 */
    @Query("select o from Order o where o.store.cafeteria.id = :cafeteriaId and "
            + "((o.orderedAt >= :from and o.orderedAt < :to) or (o.arrivedAt >= :from and o.arrivedAt < :to) "
            + "or (o.receivedAt >= :from and o.receivedAt < :to))")
    List<Order> findActiveInRange(@Param("cafeteriaId") Long cafeteriaId,
                                  @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** 대시보드/통계용: 기간 내 주문 수 (excluded 상태 제외) */
    @Query("select count(o) from Order o where o.store.cafeteria.id = :cafeteriaId "
            + "and o.orderedAt >= :from and o.orderedAt < :to and o.status <> :excluded")
    long countOrdered(@Param("cafeteriaId") Long cafeteriaId, @Param("from") LocalDateTime from,
                      @Param("to") LocalDateTime to, @Param("excluded") OrderStatus excluded);

    /** [storeId, 주문 수] */
    @Query("select o.store.id, count(o) from Order o where o.store.cafeteria.id = :cafeteriaId "
            + "and o.orderedAt >= :from and o.orderedAt < :to and o.status <> :excluded group by o.store.id")
    List<Object[]> countOrderedByStore(@Param("cafeteriaId") Long cafeteriaId, @Param("from") LocalDateTime from,
                                       @Param("to") LocalDateTime to, @Param("excluded") OrderStatus excluded);

    /** [storeId, 조리 중인 주문 수] */
    @Query("select o.store.id, count(o) from Order o where o.store.cafeteria.id = :cafeteriaId "
            + "and o.status = :status and o.expectedReadyAt > :now group by o.store.id")
    List<Object[]> countWaitingByStore(@Param("cafeteriaId") Long cafeteriaId, @Param("status") OrderStatus status,
                                       @Param("now") LocalDateTime now);

    /** 오늘 도착 인증했고 아직 수령하지 않은 주문 (도착이 최근인 순) */
    @Query("select o from Order o where o.store.cafeteria.id = :cafeteriaId and o.arrivalDate = :date "
            + "and o.status in :statuses order by o.arrivedAt desc, o.id desc")
    List<Order> findWaitings(@Param("cafeteriaId") Long cafeteriaId, @Param("date") LocalDate date,
                             @Param("statuses") Collection<OrderStatus> statuses);

    /** 시간대별 이용 인원 계산용: 이용 구간(수령 ~ 이용 종료)이 기간과 겹치는 주문 */
    @Query("select o from Order o where o.store.cafeteria.id = :cafeteriaId and o.receivedAt is not null "
            + "and o.receivedAt < :to and o.leaveAt > :from")
    List<Order> findUsageIntervals(@Param("cafeteriaId") Long cafeteriaId, @Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to);

    @Query("select o from Order o where o.store.cafeteria.id = :cafeteriaId and o.status = :status "
            + "and o.expectedReadyAt > :now order by o.store.id, o.waitingNumber")
    List<Order> findCooking(@Param("cafeteriaId") Long cafeteriaId, @Param("status") OrderStatus status,
                            @Param("now") LocalDateTime now);

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
