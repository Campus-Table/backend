package com.campustable.campus_table.dto;

import com.campustable.campus_table.entity.Menu;

public record MenuResponse(
        Long id,
        Long storeId,
        String name,
        int price,
        String imageUrl,
        boolean available
) {

    public static MenuResponse from(Menu menu) {
        return new MenuResponse(
                menu.getId(),
                menu.getStore().getId(),
                menu.getName(),
                menu.getPrice(),
                menu.getImageUrl(),
                menu.isAvailable()
        );
    }
}