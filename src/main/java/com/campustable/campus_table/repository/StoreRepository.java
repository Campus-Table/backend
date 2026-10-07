package com.campustable.campus_table.repository;

import com.campustable.campus_table.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoreRepository extends JpaRepository<Store, Long> {

    List<Store> findByCafeteriaId(Long cafeteriaId);
}