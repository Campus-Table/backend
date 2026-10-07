package com.campustable.campus_table.service;

import com.campustable.campus_table.common.CustomException;
import com.campustable.campus_table.common.ErrorCode;
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
            throw new CustomException(ErrorCode.CAFETERIA_NOT_FOUND);
        }

        return storeRepository.findByCafeteriaId(cafeteriaId)
                .stream()
                .map(StoreResponse::from)
                .toList();
    }

    public StoreResponse getStore(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new CustomException(ErrorCode.STORE_NOT_FOUND));

        return StoreResponse.from(store);
    }
}