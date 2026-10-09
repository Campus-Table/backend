package com.campustable.campus_table.service;

import com.campustable.campus_table.common.*;
import com.campustable.campus_table.dto.AdminDtos.*;
import com.campustable.campus_table.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class AdminImageService {
    private final MenuRepository menus;
    private final StoreRepository stores;
    private final ImageStorageService storage;

    @Transactional
    public MenuAdminResponse uploadMenu(Long id, MultipartFile file) {
        var menu = menus.findForUpdate(id).orElseThrow(() -> new CustomException(ErrorCode.MENU_NOT_FOUND));
        var image = storage.upload("menus", file);
        storage.cleanupAfterCommit(menu.getImageObjectKey());
        menu.changeImage(image.url(), image.key());
        return MenuAdminResponse.from(menu);
    }

    @Transactional
    public StoreAdminResponse uploadStore(Long id, MultipartFile file) {
        var store = stores.findForUpdate(id).orElseThrow(() -> new CustomException(ErrorCode.STORE_NOT_FOUND));
        var image = storage.upload("stores", file);
        storage.cleanupAfterCommit(store.getImageObjectKey());
        store.changeImage(image.url(), image.key());
        return StoreAdminResponse.from(store);
    }

    @Transactional
    public StoreAdminResponse updateStore(Long id, StoreRequest req) {
        var store = stores.findForUpdate(id).orElseThrow(() -> new CustomException(ErrorCode.STORE_NOT_FOUND));
        store.update(req.name().trim(), req.description(), req.category(), req.avgWaitMinutes());
        return StoreAdminResponse.from(store);
    }
}
