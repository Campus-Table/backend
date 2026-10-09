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
import java.util.Objects;
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
    private final ImageStorageService imageStorage;

    @Transactional
    public StoreResponse updateStore(Long storeId, StoreUpdateRequest req) {
        if (req.name() == null && req.description() == null && req.category() == null
                && req.avgWaitMinutes() == null && req.imageUrl() == null) {
            throw noChanges();
        }
        if (req.name() != null && req.name().isBlank()) {
            throw new CustomException(ErrorCode.VALIDATION_FAILED, "가게 이름은 비워둘 수 없습니다.");
        }
        // 이미지 업로드/교체와 같은 행 잠금을 사용해, 동시에 수정해도 이전 파일 정리가 어긋나지 않게 한다
        Store store = storeRepository.findForUpdate(storeId)
                .orElseThrow(() -> new CustomException(ErrorCode.STORE_NOT_FOUND));
        store.patch(req.name() == null ? null : req.name().trim(), req.description(), req.category(),
                req.avgWaitMinutes());
        if (req.imageUrl() != null) {
            String newUrl = req.imageUrl().isBlank() ? null : req.imageUrl().trim();
            if (!Objects.equals(store.getImageUrl(), newUrl)) {
                // 이전에 업로드한 파일이면 커밋 후 삭제하고 키를 지운다 (외부 URL이면 키가 없어 아무 일도 하지 않음)
                imageStorage.cleanupAfterCommit(store.getImageObjectKey());
                store.changeImage(newUrl, null);
            }
        }
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
