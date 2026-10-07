package com.campustable.campus_table.service;

import com.campustable.campus_table.dto.CafeteriaResponse;
import com.campustable.campus_table.dto.CafeteriaStatusResponse;
import com.campustable.campus_table.entity.Cafeteria;
import com.campustable.campus_table.entity.UsageSnapshot;
import com.campustable.campus_table.repository.CafeteriaRepository;
import com.campustable.campus_table.repository.UsageSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CafeteriaService {

    private final CafeteriaRepository cafeteriaRepository;
    private final UsageSnapshotRepository usageSnapshotRepository;

    public List<CafeteriaResponse> getCafeterias() {
        return cafeteriaRepository.findAll()
                .stream()
                .map(CafeteriaResponse::from)
                .toList();
    }

    public CafeteriaResponse getCafeteria(Long cafeteriaId) {
        Cafeteria cafeteria = cafeteriaRepository.findById(cafeteriaId)
                .orElseThrow(() -> new IllegalArgumentException("해당 식당을 찾을 수 없습니다."));

        return CafeteriaResponse.from(cafeteria);
    }

    public CafeteriaStatusResponse getCafeteriaStatus(Long cafeteriaId) {
        Cafeteria cafeteria = cafeteriaRepository.findById(cafeteriaId)
                .orElseThrow(() -> new IllegalArgumentException("해당 식당을 찾을 수 없습니다."));

        UsageSnapshot snapshot = usageSnapshotRepository
                .findTopByCafeteriaIdOrderByRecordedAtDesc(cafeteriaId)
                .orElseThrow(() -> new IllegalArgumentException("혼잡도 정보가 없습니다."));

        return CafeteriaStatusResponse.from(cafeteria, snapshot);
    }
}