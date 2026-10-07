package com.campustable.campus_table.repository;

import com.campustable.campus_table.entity.ArrivalCode;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArrivalCodeRepository extends JpaRepository<ArrivalCode, Long> {

    Optional<ArrivalCode> findByCafeteriaIdAndCodeDate(Long cafeteriaId, LocalDate codeDate);
}
