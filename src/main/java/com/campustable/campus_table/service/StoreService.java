package com.campustable.campus_table.service;

import com.campustable.campus_table.common.CustomException;
import com.campustable.campus_table.common.ErrorCode;
import com.campustable.campus_table.dto.StoreResponse;
import com.campustable.campus_table.entity.Menu;
import com.campustable.campus_table.entity.Store;
import com.campustable.campus_table.repository.CafeteriaRepository;
import com.campustable.campus_table.repository.MenuRepository;
import com.campustable.campus_table.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;
    private final CafeteriaRepository cafeteriaRepository;
    private final MenuRepository menuRepository;

    @Transactional(readOnly = true)
    public List<StoreResponse> getStoresByCafeteria(Long cafeteriaId) {
        if (!cafeteriaRepository.existsById(cafeteriaId)) {
            throw new CustomException(ErrorCode.CAFETERIA_NOT_FOUND);
        }

        List<Store> stores = storeRepository.findByCafeteriaId(cafeteriaId);
        // 가게마다 메뉴를 조회하지 않고 한 번에 가져온다 (N+1 방지)
        Map<Long, List<Menu>> menusByStore = menuRepository
                .findByStoreIdIn(stores.stream().map(store -> store.getId()).toList()).stream()
                .collect(Collectors.groupingBy(menu -> menu.getStore().getId()));

        return stores.stream()
                .map(store -> StoreResponse.from(store, menusByStore.getOrDefault(store.getId(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public StoreResponse getStore(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new CustomException(ErrorCode.STORE_NOT_FOUND));

        return StoreResponse.from(store, menuRepository.findByStoreId(storeId));
    }
}
