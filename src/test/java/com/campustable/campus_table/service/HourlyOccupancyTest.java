package com.campustable.campus_table.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.campustable.campus_table.service.HourlyOccupancy.Interval;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class HourlyOccupancyTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 8);

    private static Interval iv(int h1, int m1, int h2, int m2) {
        return new Interval(DAY.atTime(h1, m1), DAY.atTime(h2, m2));
    }

    @Test
    void noIntervalsIsZero() {
        var hours = HourlyOccupancy.compute(List.of(), DAY);
        assertEquals(24, hours.size());
        assertEquals(0, hours.get(12).peak());
        assertEquals(0.0, hours.get(12).avg());
    }

    @Test
    void singleIntervalInsideHour() {
        var hours = HourlyOccupancy.compute(List.of(iv(12, 10, 12, 40)), DAY);
        assertEquals(1, hours.get(12).peak());
        assertEquals(0.5, hours.get(12).avg());
        assertEquals(0, hours.get(11).peak());
        assertEquals(0, hours.get(13).peak());
    }

    @Test
    void overlappingIntervalsRaisePeak() {
        var hours = HourlyOccupancy.compute(List.of(iv(12, 0, 12, 30), iv(12, 15, 12, 45)), DAY);
        assertEquals(2, hours.get(12).peak());
        assertEquals(1.0, hours.get(12).avg()); // (30분 + 30분) / 60분
    }

    @Test
    void intervalSpanningHoursCountsInBoth() {
        var hours = HourlyOccupancy.compute(List.of(iv(11, 50, 12, 10)), DAY);
        assertEquals(1, hours.get(11).peak());
        assertEquals(0.2, hours.get(11).avg()); // 10분 → 0.1667 → 0.2
        assertEquals(1, hours.get(12).peak());
        assertEquals(0.2, hours.get(12).avg());
    }

    @Test
    void endAndStartAtSameInstantDoNotOverlap() {
        var hours = HourlyOccupancy.compute(List.of(iv(12, 0, 12, 30), iv(12, 30, 13, 0)), DAY);
        assertEquals(1, hours.get(12).peak());
        assertEquals(1.0, hours.get(12).avg());
    }

    @Test
    void intervalStartingBeforeTheDayStillCounts() {
        var prev = new Interval(LocalDateTime.of(2026, 10, 7, 23, 50), DAY.atTime(0, 20));
        var hours = HourlyOccupancy.compute(List.of(prev), DAY);
        assertEquals(1, hours.get(0).peak());
        assertEquals(0.3, hours.get(0).avg()); // 20분 → 0.333 → 0.3
    }
}
