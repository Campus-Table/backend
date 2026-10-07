package com.campustable.campus_table.service;

import com.campustable.campus_table.dto.MenuResponse;
import com.campustable.campus_table.repository.MenuRepository;
import com.campustable.campus_table.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository menuRepository;
    private final StoreRepository storeRepository;

    public List<MenuResponse> getMenusByStore(Long storeId) {
        if (!storeRepository.existsById(storeId)) {
            throw new IllegalArgumentException("해당 매장을 찾을 수 없습니다.");
        }

        return menuRepository.findByStoreId(storeId)
                .stream()
                .map(MenuResponse::from)
                .toList();
    }
}