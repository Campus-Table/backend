package com.campustable.campus_table.dto;

import com.campustable.campus_table.service.OccupancyService.Occupancy;

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

    public static CafeteriaStatusResponse from(Long cafeteriaId, Occupancy occupancy, LocalDateTime now) {
        return new CafeteriaStatusResponse(
                cafeteriaId,
                occupancy.seatCount(),
                occupancy.currentPeople(),
                occupancy.waitingPeople(),
                occupancy.usageRate(),
                occupancy.congestionLevel().name(),
                now
        );
    }
}
