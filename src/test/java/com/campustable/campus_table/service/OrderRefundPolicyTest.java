package com.campustable.campus_table.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.campustable.campus_table.entity.*;
import com.campustable.campus_table.repository.*;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class OrderRefundPolicyTest {
    @Test void expiryBoundaryAndRepeatedProcessing() {
        var orders = mock(OrderRepository.class);
        var users = mock(UserRepository.class);
        var mileage = mock(MileageService.class);
        var notifications = mock(NotificationService.class);
        var service = new OrderService(orders, mock(OrderItemRepository.class), mock(StoreRepository.class),
                mock(MenuRepository.class), users, mileage, mock(ArrivalCodeService.class), notifications, mock(EntityManager.class));
        var user = User.builder().id(1L).build();
        var at = LocalDateTime.of(2026, 10, 9, 23, 30);
        var order = Order.builder().id(2L).user(user).status(OrderStatus.PAID).totalPrice(5500).orderedAt(at).build();
        when(orders.findById(2L)).thenReturn(Optional.of(order));
        when(orders.findByIdForUpdate(2L)).thenReturn(Optional.of(order));
        when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        service.expireOrder(2L, at.plusMinutes(30));
        assertEquals(OrderStatus.PAID, order.getStatus());
        service.expireOrder(2L, at.plusMinutes(59));
        assertEquals(OrderStatus.PAID, order.getStatus());
        verifyNoInteractions(mileage);
        service.expireOrder(2L, at.plusHours(1));
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        service.expireOrder(2L, at.plusHours(2));
        verify(mileage, times(1)).apply(1L, MileageType.REFUND, 5500, order);
    }
}
