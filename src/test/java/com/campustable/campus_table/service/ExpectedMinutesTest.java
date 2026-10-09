package com.campustable.campus_table.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ExpectedMinutesTest {
    @Test
    void capacityOneKeepsPreviousFormula() {
        assertEquals(2, OrderService.expectedMinutes(0, 1, 2));
        assertEquals(6, OrderService.expectedMinutes(2, 1, 2)); // (앞 2명 + 1) x 2분
    }

    @Test
    void cooksInParallelUpToCapacity() {
        assertEquals(2, OrderService.expectedMinutes(0, 3, 2));
        assertEquals(2, OrderService.expectedMinutes(2, 3, 2)); // 세 번째 주문까지 바로 조리
        assertEquals(4, OrderService.expectedMinutes(3, 3, 2)); // 네 번째는 한 바퀴 기다림
        assertEquals(4, OrderService.expectedMinutes(5, 3, 2));
        assertEquals(6, OrderService.expectedMinutes(6, 3, 2));
    }

    @Test
    void invalidCapacityFallsBackToOne() {
        assertEquals(6, OrderService.expectedMinutes(2, 0, 2));
    }
}
