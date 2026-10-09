package com.campustable.campus_table.service;

import com.campustable.campus_table.common.CustomException;
import com.campustable.campus_table.common.ErrorCode;
import com.campustable.campus_table.dto.AdminDtos.ArrivalCodeResponse;
import com.campustable.campus_table.dto.AdminDtos.CafeteriaUpdateRequest;
import com.campustable.campus_table.dto.AdminDtos.StoreUpdateRequest;
import com.campustable.campus_table.dto.CafeteriaResponse;
import com.campustable.campus_table.dto.StoreResponse;
import com.campustable.campus_table.entity.Cafeteria;
import com.campustable.campus_table.entity.Store;
import com.campustable.campus_table.repository.CafeteriaRepository;
import com.campustable.campus_table.repository.MenuRepository;
import com.campustable.campus_table.repository.StoreRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 관리자의 식당/가게 설정 수정과 현장 번호 재발급 */
@Service
@RequiredArgsConstructor
public class AdminSettingsService {

    private final StoreRepository storeRepository;
    private final CafeteriaRepository cafeteriaRepository;
    private final MenuRepository menuRepository;
    private final ArrivalCodeService arrivalCodeService;

    @Transactional
    public StoreResponse updateStore(Long storeId, StoreUpdateRequest req) {
        if (req.name() == null && req.description() == null && req.category() == null
                && req.avgWaitMinutes() == null && req.imageUrl() == null) {
            throw noChanges();
        }
        if (req.name() != null && req.name().isBlank()) {
            throw new CustomException(ErrorCode.VALIDATION_FAILED, "가게 이름은 비워둘 수 없습니다.");
        }
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new CustomException(ErrorCode.STORE_NOT_FOUND));
        store.update(req.name() == null ? null : req.name().trim(), req.description(), req.category(),
                req.avgWaitMinutes(), req.imageUrl());
        return StoreResponse.from(store, menuRepository.findByStoreId(storeId));
    }

    @Transactional
    public CafeteriaResponse updateCafeteria(Long cafeteriaId, CafeteriaUpdateRequest req) {
        if (req.name() == null && req.seatCount() == null && req.diningMinutes() == null
                && req.openingTime() == null && req.closingTime() == null) {
            throw noChanges();
        }
        if (req.name() != null && req.name().isBlank()) {
            throw new CustomException(ErrorCode.VALIDATION_FAILED, "식당 이름은 비워둘 수 없습니다.");
        }
        Cafeteria cafeteria = cafeteriaRepository.findById(cafeteriaId)
                .orElseThrow(() -> new CustomException(ErrorCode.CAFETERIA_NOT_FOUND));

        // 일부만 보내도 변경 후의 영업시간이 올바른지 확인한다
        LocalTime open = req.openingTime() != null ? req.openingTime() : cafeteria.getOpeningTime();
        LocalTime close = req.closingTime() != null ? req.closingTime() : cafeteria.getClosingTime();
        if (open != null && close != null && !open.isBefore(close)) {
            throw new CustomException(ErrorCode.VALIDATION_FAILED, "영업 종료 시간은 시작 시간보다 늦어야 합니다.");
        }
        cafeteria.update(req.name() == null ? null : req.name().trim(), req.seatCount(), req.diningMinutes(),
                req.openingTime(), req.closingTime());
        return CafeteriaResponse.from(cafeteria);
    }

    public ArrivalCodeResponse regenerateArrivalCode(Long cafeteriaId) {
        LocalDate today = LocalDate.now();
        return new ArrivalCodeResponse(cafeteriaId, today,
                arrivalCodeService.regenerate(cafeteriaId, today).getCode());
    }

    private CustomException noChanges() {
        return new CustomException(ErrorCode.VALIDATION_FAILED, "변경할 항목이 없습니다.");
    }
}
