package com.campustable.campus_table.service;

import com.campustable.campus_table.common.CustomException;
import com.campustable.campus_table.common.ErrorCode;
import com.campustable.campus_table.dto.AdminDtos.AvailabilityRequest;
import com.campustable.campus_table.dto.AdminDtos.MenuAdminResponse;
import com.campustable.campus_table.dto.AdminDtos.MenuRequest;
import com.campustable.campus_table.entity.Menu;
import com.campustable.campus_table.entity.Store;
import com.campustable.campus_table.repository.MenuRepository;
import com.campustable.campus_table.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 주문 내역이 참조하므로 삭제 대신 판매 중지(available=false)로 관리한다. */
@Service
@RequiredArgsConstructor
public class AdminMenuService {

    private final ImageStorageService imageStorage;
    private final MenuRepository menuRepository;
    private final StoreRepository storeRepository;

    @Transactional
    public MenuAdminResponse create(Long storeId, MenuRequest req) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new CustomException(ErrorCode.STORE_NOT_FOUND));
        return MenuAdminResponse.from(menuRepository.save(Menu.builder()
                .store(store).name(req.name().trim()).price(req.price()).imageUrl(req.imageUrl()).build()));
    }

    @Transactional
    public MenuAdminResponse update(Long menuId, MenuRequest req) {
        Menu menu = find(menuId);
        if (!java.util.Objects.equals(menu.getImageUrl(), req.imageUrl())) {
            imageStorage.cleanupAfterCommit(menu.getImageObjectKey());
            menu.changeImage(req.imageUrl(), null);
        }
        menu.update(req.name().trim(), req.price(), req.imageUrl());
        return MenuAdminResponse.from(menu);
    }

    @Transactional
    public MenuAdminResponse changeAvailability(Long menuId, AvailabilityRequest req) {
        Menu menu = find(menuId);
        menu.changeAvailable(req.available());
        return MenuAdminResponse.from(menu);
    }

    private Menu find(Long menuId) {
        return menuRepository.findForUpdate(menuId).orElseThrow(() -> new CustomException(ErrorCode.MENU_NOT_FOUND));
    }
}
