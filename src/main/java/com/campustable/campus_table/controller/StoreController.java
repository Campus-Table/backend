package com.campustable.campus_table.controller;

import com.campustable.campus_table.dto.MenuResponse;
import com.campustable.campus_table.dto.StoreResponse;
import com.campustable.campus_table.service.MenuService;
import com.campustable.campus_table.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stores/{storeId}")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;
    private final MenuService menuService;

    @GetMapping
    public StoreResponse getStore(
            @PathVariable Long storeId
    ) {
        return storeService.getStore(storeId);
    }

    @GetMapping("/menus")
    public List<MenuResponse> getMenus(
            @PathVariable Long storeId
    ) {
        return menuService.getMenusByStore(storeId);
    }
}