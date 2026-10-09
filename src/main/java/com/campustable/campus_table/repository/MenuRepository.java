package com.campustable.campus_table.repository;

import com.campustable.campus_table.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface MenuRepository extends JpaRepository<Menu, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select e from Menu e where e.id = :id")
    java.util.Optional<Menu> findForUpdate(@org.springframework.data.repository.query.Param("id") Long id);


    List<Menu> findByStoreId(Long storeId);

    List<Menu> findByStoreIdIn(Collection<Long> storeIds);
}
