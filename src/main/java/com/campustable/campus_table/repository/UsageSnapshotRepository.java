package com.campustable.campus_table.repository;

import com.campustable.campus_table.entity.UsageSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsageSnapshotRepository extends JpaRepository<UsageSnapshot, Long> {

    Optional<UsageSnapshot> findTopByCafeteriaIdOrderByRecordedAtDesc(Long cafeteriaId);
}