package com.stockpro.warehouse.repository;

import com.stockpro.warehouse.entity.StockLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockLevelRepository extends JpaRepository<StockLevel, Long> {

    Optional<StockLevel> findByWarehouseIdAndProductId(Long warehouseId, Long productId);

    List<StockLevel> findByWarehouseId(Long warehouseId);

    List<StockLevel> findByProductId(Long productId);

    boolean existsByWarehouseIdAndProductId(Long warehouseId, Long productId);



    // Total quantity of a product across ALL warehouses
    @Query("SELECT COALESCE(SUM(sl.quantity), 0) FROM StockLevel sl WHERE sl.productId = :productId")
    int sumQuantityByProductId(@Param("productId") Long productId);
}
