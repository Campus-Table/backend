package com.campustable.campus_table.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 이용 구간(수령 ~ 이용 종료) 목록에서 시간대별 최대·평균 이용 인원을 계산한다.
 * 구간은 [start, end) 이며 같은 시각에 끝나고 시작하는 구간은 겹치지 않는 것으로 본다.
 */
public final class HourlyOccupancy {

    public record Interval(LocalDateTime start, LocalDateTime end) {
    }

    public record Hour(int peak, double avg) {
    }

    private record Event(LocalDateTime at, int delta) {
    }

    private HourlyOccupancy() {
    }

    /** 0~23시 24개 항목. avg는 그 시간(60분) 동안의 시간 가중 평균으로 소수 첫째 자리까지. */
    public static List<Hour> compute(List<Interval> intervals, LocalDate day) {
        List<Hour> result = new ArrayList<>(24);
        for (int h = 0; h < 24; h++) {
            LocalDateTime from = day.atTime(h, 0);
            LocalDateTime to = from.plusHours(1);

            int active = 0;
            long seconds = 0;
            List<Event> events = new ArrayList<>();
            for (Interval iv : intervals) {
                if (!iv.start().isBefore(to) || !iv.end().isAfter(from) || !iv.end().isAfter(iv.start())) {
                    continue;
                }
                LocalDateTime s = iv.start().isAfter(from) ? iv.start() : from;
                LocalDateTime e = iv.end().isBefore(to) ? iv.end() : to;
                seconds += Duration.between(s, e).getSeconds();

                if (iv.start().isAfter(from)) {
                    events.add(new Event(iv.start(), +1));
                } else {
                    active++;
                }
                if (iv.end().isBefore(to)) {
                    events.add(new Event(iv.end(), -1));
                }
            }

            events.sort(Comparator.comparing((Event event) -> event.at()).thenComparingInt(event -> event.delta())); // 같은 시각이면 종료(-1)가 먼저
            int peak = active;
            for (Event ev : events) {
                active += ev.delta();
                peak = Math.max(peak, active);
            }
            result.add(new Hour(peak, Math.round(seconds / 3600.0 * 10) / 10.0));
        }
        return result;
    }
}
