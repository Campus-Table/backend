package com.campustable.campus_table.controller;

import com.campustable.campus_table.dto.OccupancyDtos.*;
import com.campustable.campus_table.entity.OccupancySource;
import com.campustable.campus_table.service.*;
import jakarta.validation.Valid;
import java.time.*;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/cafeterias/{cafeteriaId}/occupancy")
@RequiredArgsConstructor
public class OccupancyAdminController {
    private final OccupancySnapshotService snapshots;
    private final PeopleEstimator estimator;

    @PostMapping(value = "/analyze", consumes = "multipart/form-data")
    public SnapshotResponse analyze(@PathVariable Long cafeteriaId, @RequestParam("image") MultipartFile image) {
        snapshots.requireCafeteria(cafeteriaId);
        // Capture time before network call; no DB transaction held during inference.
        LocalDateTime capturedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        int count = estimator.estimate(image);
        return snapshots.save(cafeteriaId, count, capturedAt, OccupancySource.CLOVA);
    }

    @PostMapping
    public SnapshotResponse manual(@PathVariable Long cafeteriaId, @Valid @RequestBody Input input) {
        return snapshots.manual(cafeteriaId, input);
    }

    @PostMapping("/simulation")
    public List<SnapshotResponse> simulate(@PathVariable Long cafeteriaId, @Valid @RequestBody SimulationRequest input) {
        return snapshots.simulate(cafeteriaId, input);
    }

    @GetMapping("/history")
    public List<SnapshotResponse> history(@PathVariable Long cafeteriaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return snapshots.history(cafeteriaId, date);
    }
}
