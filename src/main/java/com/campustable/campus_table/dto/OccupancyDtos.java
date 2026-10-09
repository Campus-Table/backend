package com.campustable.campus_table.dto;

import com.campustable.campus_table.entity.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;

public final class OccupancyDtos {
    private OccupancyDtos() { }
    public record Input(@NotNull @Min(0) @Max(10000) Integer currentPeople,
                        LocalDateTime recordedAt, OccupancySource source) { }
    public record SimulationRequest(@NotEmpty @Size(max = 500) List<@NotNull @Valid Input> snapshots) { }
    public record SnapshotResponse(Long snapshotId, Long cafeteriaId, int peopleCount,
                                   OccupancySource source, LocalDateTime recordedAt) {
        public static SnapshotResponse from(UsageSnapshot s) {
            return new SnapshotResponse(s.getId(), s.getCafeteria().getId(), s.getCurrentPeople(),
                    s.getSource() == null ? OccupancySource.MANUAL : s.getSource(), s.getRecordedAt());
        }
    }
}
