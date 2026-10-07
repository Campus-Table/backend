package com.campustable.campus_table.repository;

import com.campustable.campus_table.entity.MileageTransaction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MileageTransactionRepository extends JpaRepository<MileageTransaction, Long> {

    List<MileageTransaction> findByUserIdOrderByIdDesc(Long userId);
}
