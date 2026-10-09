package com.campustable.campus_table.controller;

import com.campustable.campus_table.dto.AdminDtos.*;
import com.campustable.campus_table.entity.OrderStatus;
import com.campustable.campus_table.dto.CafeteriaResponse;
import com.campustable.campus_table.dto.StoreResponse;
import com.campustable.campus_table.service.AdminMenuService;
import com.campustable.campus_table.service.AdminSettingsService;
import com.campustable.campus_table.service.AdminService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** /api/admin/** 은 SecurityConfig에서 ADMIN 권한만 허용한다. */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final AdminMenuService menuService;
    private final AdminSettingsService settingsService;

    @PatchMapping("/cafeterias/{cafeteriaId}")
    public CafeteriaResponse updateCafeteria(@PathVariable Long cafeteriaId,
                                             @Valid @RequestBody CafeteriaUpdateRequest req) {
        return settingsService.updateCafeteria(cafeteriaId, req);
    }

    @PatchMapping("/stores/{storeId}")
    public StoreResponse updateStore(@PathVariable Long storeId, @Valid @RequestBody StoreUpdateRequest req) {
        return settingsService.updateStore(storeId, req);
    }

    @PostMapping("/cafeterias/{cafeteriaId}/arrival-code/regenerate")
    public ArrivalCodeResponse regenerateArrivalCode(@PathVariable Long cafeteriaId) {
        return settingsService.regenerateArrivalCode(cafeteriaId);
    }

    @GetMapping("/cafeterias/{cafeteriaId}/arrival-code")
    public ArrivalCodeResponse arrivalCode(@PathVariable Long cafeteriaId) {
        return adminService.arrivalCode(cafeteriaId);
    }

    @GetMapping("/cafeterias/{cafeteriaId}/dashboard")
    public DashboardResponse dashboard(@PathVariable Long cafeteriaId) {
        return adminService.dashboard(cafeteriaId);
    }

    @GetMapping("/cafeterias/{cafeteriaId}/store-stats")
    public List<StoreStat> storeStats(@PathVariable Long cafeteriaId) {
        return adminService.storeStats(cafeteriaId);
    }

    @GetMapping("/cafeterias/{cafeteriaId}/waitings")
    public List<WaitingOrder> waitings(@PathVariable Long cafeteriaId) {
        return adminService.waitings(cafeteriaId);
    }

    @GetMapping("/cafeterias/{cafeteriaId}/queues")
    public List<StoreQueue> queues(@PathVariable Long cafeteriaId) {
        return adminService.queues(cafeteriaId);
    }

    @GetMapping("/cafeterias/{cafeteriaId}/usage/hourly")
    public List<HourlyUsage> hourly(@PathVariable Long cafeteriaId,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return adminService.hourlyUsage(cafeteriaId, date);
    }

    @GetMapping("/orders")
    public List<AdminOrderResponse> orders(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) OrderStatus status) {
        return adminService.orders(date, storeId, status);
    }

    @GetMapping("/orders/menu-counts")
    public List<MenuOrderCount> menuCounts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long storeId) {
        return adminService.menuCounts(date, storeId);
    }

    @PostMapping("/stores/{storeId}/menus")
    @ResponseStatus(HttpStatus.CREATED)
    public MenuAdminResponse createMenu(@PathVariable Long storeId, @Valid @RequestBody MenuRequest req) {
        return menuService.create(storeId, req);
    }

    @PutMapping("/menus/{menuId}")
    public MenuAdminResponse updateMenu(@PathVariable Long menuId, @Valid @RequestBody MenuRequest req) {
        return menuService.update(menuId, req);
    }

    @PatchMapping("/menus/{menuId}/availability")
    public MenuAdminResponse changeAvailability(@PathVariable Long menuId,
                                                @Valid @RequestBody AvailabilityRequest req) {
        return menuService.changeAvailability(menuId, req);
    }
}
