package com.campustable.campus_table.service;

import com.campustable.campus_table.dto.StoreResponse;
import com.campustable.campus_table.entity.Store;
import com.campustable.campus_table.repository.CafeteriaRepository;
import com.campustable.campus_table.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;
    private final CafeteriaRepository cafeteriaRepository;

    public List<StoreResponse> getStoresByCafeteria(Long cafeteriaId) {
        if (!cafeteriaRepository.existsById(cafeteriaId)) {
            throw new IllegalArgumentException("해당 식당을 찾을 수 없습니다.");
        }

        return storeRepository.findByCafeteriaId(cafeteriaId)
                .stream()
                .map(StoreResponse::from)
                .toList();
    }

    public StoreResponse getStore(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 매장을 찾을 수 없습니다."));

        return StoreResponse.from(store);
    }
}