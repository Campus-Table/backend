package com.campustable.campus_table.dto;

import com.campustable.campus_table.entity.Cafeteria;

import java.time.LocalTime;

public record CafeteriaResponse(
        Long id,
        String name,
        int seatCount,
        int diningMinutes,
        LocalTime openingTime,
        LocalTime closingTime
) {

    public static CafeteriaResponse from(Cafeteria cafeteria) {
        return new CafeteriaResponse(
                cafeteria.getId(),
                cafeteria.getName(),
                cafeteria.getSeatCount(),
                cafeteria.getDiningMinutes(),
                cafeteria.getOpeningTime(),
                cafeteria.getClosingTime()
        );
    }
}