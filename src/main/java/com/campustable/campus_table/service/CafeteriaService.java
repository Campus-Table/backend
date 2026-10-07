package com.campustable.campus_table.service;

import com.campustable.campus_table.common.CustomException;
import com.campustable.campus_table.common.ErrorCode;
import com.campustable.campus_table.dto.CafeteriaResponse;
import com.campustable.campus_table.dto.CafeteriaStatusResponse;
import com.campustable.campus_table.entity.Cafeteria;
import com.campustable.campus_table.repository.CafeteriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CafeteriaService {

    private final CafeteriaRepository cafeteriaRepository;
    private final OccupancyService occupancyService;

    public List<CafeteriaResponse> getCafeterias() {
        return cafeteriaRepository.findAll()
                .stream()
                .map(CafeteriaResponse::from)
                .toList();
    }

    public CafeteriaResponse getCafeteria(Long cafeteriaId) {
        Cafeteria cafeteria = cafeteriaRepository.findById(cafeteriaId)
                .orElseThrow(() -> new CustomException(ErrorCode.CAFETERIA_NOT_FOUND));

        return CafeteriaResponse.from(cafeteria);
    }

    /** 현재 이용 인원/대기 인원은 주문 데이터에서 실시간으로 계산한다. */
    public CafeteriaStatusResponse getCafeteriaStatus(Long cafeteriaId) {
        return CafeteriaStatusResponse.from(
                cafeteriaId, occupancyService.occupancy(cafeteriaId), LocalDateTime.now());
    }
}
