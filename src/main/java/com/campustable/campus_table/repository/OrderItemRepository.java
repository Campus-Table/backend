package com.campustable.campus_table.repository;

import com.campustable.campus_table.entity.OrderItem;
import com.campustable.campus_table.entity.OrderStatus;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderIdIn(Collection<Long> orderIds);

    /** 가게별 메뉴 주문 수 (학식당 전체, 취소 제외): [storeId, menuId, menuName, 주문 수] */
    @Query("select i.menu.store.id, i.menu.id, i.menu.name, count(distinct i.order.id) "
            + "from OrderItem i where i.menu.store.cafeteria.id = :cafeteriaId "
            + "and i.order.orderedAt >= :from and i.order.orderedAt < :to and i.order.status <> :excluded "
            + "group by i.menu.store.id, i.menu.id, i.menu.name")
    List<Object[]> menuOrderCountsByCafeteria(@Param("cafeteriaId") Long cafeteriaId,
                                              @Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                                              @Param("excluded") OrderStatus excluded);

    /** 메뉴별 주문 현황: [menuId, menuName, storeId, storeName, 주문 수, 총 수량] (취소 제외) */
    @Query("select i.menu.id, i.menu.name, i.menu.store.id, i.menu.store.name, count(distinct i.order.id), sum(i.quantity) "
            + "from OrderItem i where i.order.orderedAt >= :from and i.order.orderedAt < :to "
            + "and i.order.status <> :excluded and (:storeId is null or i.menu.store.id = :storeId) "
            + "group by i.menu.id, i.menu.name, i.menu.store.id, i.menu.store.name "
            + "order by sum(i.quantity) desc")
    List<Object[]> menuCounts(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                              @Param("storeId") Long storeId, @Param("excluded") OrderStatus excluded);
}
