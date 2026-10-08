package com.campustable.campus_table.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CongestionLevelTest {

    @Test
    void relaxedBoundaryTest() {
        assertEquals(CongestionLevel.RELAXED, CongestionLevel.of(0.0));
        assertEquals(CongestionLevel.RELAXED, CongestionLevel.of(29.9));
    }

    @Test
    void normalBoundaryTest() {
        assertEquals(CongestionLevel.NORMAL, CongestionLevel.of(30.0));
        assertEquals(CongestionLevel.NORMAL, CongestionLevel.of(59.9));
    }

    @Test
    void crowdedBoundaryTest() {
        assertEquals(CongestionLevel.CROWDED, CongestionLevel.of(60.0));
        assertEquals(CongestionLevel.CROWDED, CongestionLevel.of(84.9));
    }

    @Test
    void veryCrowdedBoundaryTest() {
        assertEquals(CongestionLevel.VERY_CROWDED, CongestionLevel.of(85.0));
        assertEquals(CongestionLevel.VERY_CROWDED, CongestionLevel.of(100.0));
    }
}
