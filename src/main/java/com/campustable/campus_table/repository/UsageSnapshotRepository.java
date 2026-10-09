package com.campustable.campus_table.repository;

import com.campustable.campus_table.entity.UsageSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsageSnapshotRepository extends JpaRepository<UsageSnapshot, Long> {

    java.util.List<UsageSnapshot> findByCafeteriaIdAndRecordedAtGreaterThanEqualAndRecordedAtLessThanOrderByRecordedAtAscIdAsc(
            Long cafeteriaId, java.time.LocalDateTime from, java.time.LocalDateTime to);

    Optional<UsageSnapshot> findTopByCafeteriaIdAndRecordedAtLessThanEqualOrderByRecordedAtDescIdDesc(
            Long cafeteriaId, java.time.LocalDateTime now);

    Optional<UsageSnapshot> findTopByCafeteriaIdOrderByRecordedAtDesc(Long cafeteriaId);
}