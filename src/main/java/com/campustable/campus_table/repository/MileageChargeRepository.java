package com.campustable.campus_table.repository;

import com.campustable.campus_table.entity.MileageCharge;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface MileageChargeRepository extends JpaRepository<MileageCharge, String> {
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from MileageCharge c where c.id = :id")
    Optional<MileageCharge> findForUpdate(@Param("id") String id);
}
