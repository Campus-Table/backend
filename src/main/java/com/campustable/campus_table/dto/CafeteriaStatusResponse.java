package com.campustable.campus_table.dto;

import com.campustable.campus_table.entity.Cafeteria;
import com.campustable.campus_table.entity.UsageSnapshot;

import java.time.LocalDateTime;

public record CafeteriaStatusResponse(
        Long cafeteriaId,
        int seatCount,
        int currentPeople,
        int waitingPeople,
        double usageRate,
        String congestionLevel,
        LocalDateTime recordedAt
) {

    public static CafeteriaStatusResponse from(
            Cafeteria cafeteria,
            UsageSnapshot snapshot
    ) {
        double usageRate = cafeteria.getSeatCount() == 0
                ? 0.0
                : (double) snapshot.getCurrentPeople()
                / cafeteria.getSeatCount() * 100;

        String congestionLevel;

        if (usageRate < 50) {
            congestionLevel = "NORMAL";
        } else if (usageRate < 80) {
            congestionLevel = "CROWDED";
        } else {
            congestionLevel = "VERY_CROWDED";
        }

        return new CafeteriaStatusResponse(
                cafeteria.getId(),
                cafeteria.getSeatCount(),
                snapshot.getCurrentPeople(),
                snapshot.getWaitingPeople(),
                usageRate,
                congestionLevel,
                snapshot.getRecordedAt()
        );
    }
}