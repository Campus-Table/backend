package com.campustable.campus_table.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.campustable.campus_table.entity.*;
import com.campustable.campus_table.repository.*;
import java.time.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class OccupancyServiceTest {
    @Test void usesFreshSnapshotAndFallsBackForStaleOne() {
        var cafeterias = mock(CafeteriaRepository.class);
        var orders = mock(OrderRepository.class);
        var snapshots = mock(UsageSnapshotRepository.class);
        var service = new OccupancyService(cafeterias, orders, snapshots);
        ReflectionTestUtils.setField(service, "snapshotMaxAgeSeconds", 300L);
        when(cafeterias.findById(1L)).thenReturn(Optional.of(Cafeteria.builder().seatCount(100).build()));
        when(orders.countInUse(eq(1L), eq(OrderStatus.RECEIVED), any())).thenReturn(7L);
        when(snapshots.findTopByCafeteriaIdAndRecordedAtLessThanEqualOrderByRecordedAtDescIdDesc(eq(1L), any()))
                .thenReturn(Optional.of(UsageSnapshot.builder().recordedAt(LocalDateTime.now(ZoneId.of("Asia/Seoul")))
                        .currentPeople(42).build()));
        assertEquals(42, service.occupancy(1L).currentPeople());
        verify(orders, never()).countInUse(anyLong(), any(), any());
        when(snapshots.findTopByCafeteriaIdAndRecordedAtLessThanEqualOrderByRecordedAtDescIdDesc(eq(1L), any()))
                .thenReturn(Optional.of(UsageSnapshot.builder().recordedAt(LocalDateTime.now(ZoneId.of("Asia/Seoul")).minusMinutes(6))
                        .currentPeople(42).build()));
        assertEquals(7, service.occupancy(1L).currentPeople());
    }
}
