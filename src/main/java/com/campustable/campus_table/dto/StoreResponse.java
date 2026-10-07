package com.campustable.campus_table.dto;

import com.campustable.campus_table.entity.Store;

public record StoreResponse(
        Long id,
        Long cafeteriaId,
        String name,
        int avgWaitMinutes,
        String imageUrl
) {

    public static StoreResponse from(Store store) {
        return new StoreResponse(
                store.getId(),
                store.getCafeteria().getId(),
                store.getName(),
                store.getAvgWaitMinutes(),
                store.getImageUrl()
        );
    }
}