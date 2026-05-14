package com.stockpro.warehouse.repository;

import com.stockpro.warehouse.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    List<Warehouse> findByIsActive(boolean isActive);

    List<Warehouse> findByManagerId(Long managerId);

    List<Warehouse> findByLocationContainingIgnoreCase(String location);

    boolean existsByName(String name);
}
