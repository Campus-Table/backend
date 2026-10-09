package com.campustable.campus_table.repository;

import com.campustable.campus_table.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoreRepository extends JpaRepository<Store, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select e from Store e where e.id = :id")
    java.util.Optional<Store> findForUpdate(@org.springframework.data.repository.query.Param("id") Long id);


    List<Store> findByCafeteriaId(Long cafeteriaId);
}