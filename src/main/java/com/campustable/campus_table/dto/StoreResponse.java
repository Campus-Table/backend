package com.campustable.campus_table.dto;

import com.campustable.campus_table.entity.Menu;
import com.campustable.campus_table.entity.Store;

import java.util.Comparator;
import java.util.List;

public record StoreResponse(
        Long id,
        Long cafeteriaId,
        String name,
        String description,
        String category,
        int avgWaitMinutes,
        String imageUrl,
        Integer minPrice,
        String representativeMenuName
) {

    /**
     * @param menus 이 가게의 메뉴. minPrice는 판매 중(available) 메뉴의 최저가,
     *              representativeMenuName은 등록 순(ID가 가장 작은) 첫 번째 메뉴 이름이다.
     */
    public static StoreResponse from(Store store, List<Menu> menus) {
        Integer minPrice = menus.stream()
                .filter(Menu::isAvailable)
                .map(Menu::getPrice)
                .min(Integer::compare)
                .orElse(null);
        String representative = menus.stream()
                .min(Comparator.comparing(Menu::getId))
                .map(Menu::getName)
                .orElse(null);

        return new StoreResponse(
                store.getId(),
                store.getCafeteria().getId(),
                store.getName(),
                store.getDescription(),
                store.getCategory(),
                store.getAvgWaitMinutes(),
                store.getImageUrl(),
                minPrice,
                representative
        );
    }
}
