package com.campustable.campus_table.controller;

import com.campustable.campus_table.dto.CafeteriaResponse;
import com.campustable.campus_table.dto.CafeteriaStatusResponse;
import com.campustable.campus_table.dto.StoreResponse;
import com.campustable.campus_table.service.CafeteriaService;
import com.campustable.campus_table.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cafeterias")
@RequiredArgsConstructor
public class CafeteriaController {

    private final CafeteriaService cafeteriaService;
    private final StoreService storeService;

    @GetMapping
    public List<CafeteriaResponse> getCafeterias() {
        return cafeteriaService.getCafeterias();
    }

    @GetMapping("/{cafeteriaId}")
    public CafeteriaResponse getCafeteria(
            @PathVariable Long cafeteriaId
    ) {
        return cafeteriaService.getCafeteria(cafeteriaId);
    }

    @GetMapping("/{cafeteriaId}/stores")
    public List<StoreResponse> getStores(
            @PathVariable Long cafeteriaId
    ) {
        return storeService.getStoresByCafeteria(cafeteriaId);
    }

    @GetMapping("/{cafeteriaId}/status")
    public CafeteriaStatusResponse getStatus(
            @PathVariable Long cafeteriaId
    ) {
        return cafeteriaService.getCafeteriaStatus(cafeteriaId);
    }
}