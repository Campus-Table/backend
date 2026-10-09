package com.campustable.campus_table.service;

import com.campustable.campus_table.common.CongestionLevel;
import com.campustable.campus_table.common.CustomException;
import com.campustable.campus_table.common.ErrorCode;
import com.campustable.campus_table.entity.Cafeteria;
import com.campustable.campus_table.entity.OrderStatus;
import com.campustable.campus_table.repository.CafeteriaRepository;
import com.campustable.campus_table.repository.OrderRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 학식당 전체 현재 이용 인원/대기 인원/혼잡도. 주문 데이터에서 계산한다. */
@Service
@RequiredArgsConstructor
public class OccupancyService {

    public record Occupancy(int currentPeople, int waitingPeople, int seatCount,
                            double usageRate, CongestionLevel congestionLevel) {
    }

    private final CafeteriaRepository cafeteriaRepository;
    private final OrderRepository orderRepository;
    private final com.campustable.campus_table.repository.UsageSnapshotRepository snapshots;
    @org.springframework.beans.factory.annotation.Value("${app.occupancy.snapshot-max-age-seconds:300}")
    private long snapshotMaxAgeSeconds;

    @Transactional(readOnly = true)
    public Occupancy occupancy(Long cafeteriaId) {
        Cafeteria cafeteria = cafeteriaRepository.findById(cafeteriaId)
                .orElseThrow(() -> new CustomException(ErrorCode.CAFETERIA_NOT_FOUND));
        LocalDateTime now = LocalDateTime.now(java.time.ZoneId.of("Asia/Seoul"));
        int current = snapshots.findTopByCafeteriaIdAndRecordedAtLessThanEqualOrderByRecordedAtDescIdDesc(cafeteriaId, now)
                .filter(s -> !s.getRecordedAt().isBefore(now.minusSeconds(snapshotMaxAgeSeconds)))
                .map(s -> s.getCurrentPeople())
                .orElseGet(() -> (int) orderRepository.countInUse(cafeteriaId, OrderStatus.RECEIVED, now));
        int waiting = (int) orderRepository.countWaiting(cafeteriaId, OrderStatus.COOKING, now);
        double rate = cafeteria.getSeatCount() == 0 ? 0
                : Math.round(current * 1000.0 / cafeteria.getSeatCount()) / 10.0;
        return new Occupancy(current, waiting, cafeteria.getSeatCount(), rate, CongestionLevel.of(rate));
    }
}
