package com.campustable.campus_table.service;

import com.campustable.campus_table.common.*;
import com.campustable.campus_table.dto.OccupancyDtos.*;
import com.campustable.campus_table.entity.*;
import com.campustable.campus_table.repository.*;
import java.time.*;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OccupancySnapshotService {
    private final CafeteriaRepository cafeterias;
    private final UsageSnapshotRepository snapshots;
    private final OrderRepository orders;
    static LocalDateTime now() { return LocalDateTime.now(ZoneId.of("Asia/Seoul")); }

    public void requireCafeteria(Long id) {
        if (!cafeterias.existsById(id)) throw new CustomException(ErrorCode.CAFETERIA_NOT_FOUND);
    }

    @Transactional
    public SnapshotResponse save(Long id, int people, LocalDateTime at, OccupancySource source) {
        return SnapshotResponse.from(store(id, people, at, source));
    }

    private UsageSnapshot store(Long id, int people, LocalDateTime at, OccupancySource source) {
        var cafeteria = cafeterias.findById(id).orElseThrow(() -> new CustomException(ErrorCode.CAFETERIA_NOT_FOUND));
        var time = at == null ? now() : at;
        if (people < 0 || people > 10000 || time.isAfter(now())) throw new CustomException(ErrorCode.VALIDATION_FAILED);
        int waiting = (int) orders.countWaiting(id, OrderStatus.COOKING, time);
        return snapshots.save(UsageSnapshot.builder().cafeteria(cafeteria).recordedAt(time)
                .currentPeople(people).waitingPeople(waiting).source(source).build());
    }

    @Transactional
    public SnapshotResponse manual(Long id, Input input) {
        var source = input.source() == null ? OccupancySource.SIMULATION : input.source();
        if (source == OccupancySource.CLOVA) throw new CustomException(ErrorCode.VALIDATION_FAILED);
        return SnapshotResponse.from(store(id, input.currentPeople(), input.recordedAt(), source));
    }

    @Transactional
    public List<SnapshotResponse> simulate(Long id, SimulationRequest input) {
        // One transaction: invalid data rolls back the entire scenario.
        return input.snapshots().stream().map(s -> {
            if (s.source() != null && s.source() != OccupancySource.SIMULATION)
                throw new CustomException(ErrorCode.VALIDATION_FAILED);
            return SnapshotResponse.from(store(id, s.currentPeople(), s.recordedAt(), OccupancySource.SIMULATION));
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<SnapshotResponse> history(Long id, LocalDate date) {
        requireCafeteria(id);
        LocalDate day = date == null ? now().toLocalDate() : date;
        return snapshots.findByCafeteriaIdAndRecordedAtGreaterThanEqualAndRecordedAtLessThanOrderByRecordedAtAscIdAsc(
                id, day.atStartOfDay(), day.plusDays(1).atStartOfDay()).stream().map(SnapshotResponse::from).toList();
    }
}
