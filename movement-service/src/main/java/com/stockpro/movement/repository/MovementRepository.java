package com.stockpro.movement.repository;

import com.stockpro.movement.entity.StockMovement;
import com.stockpro.movement.entity.StockMovement.MovementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MovementRepository extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findByProductId(Long productId);

    List<StockMovement> findByWarehouseId(Long warehouseId);

    List<StockMovement> findByMovementType(MovementType movementType);

    List<StockMovement> findByPerformedBy(Long userId);

    List<StockMovement> findByReferenceId(Long referenceId);

    List<StockMovement> findByMovementDateBetween(LocalDateTime from, LocalDateTime to);

    // History for a specific product in a specific warehouse — ordered chronologically
    List<StockMovement> findByProductIdAndWarehouseIdOrderByMovementDateAsc(
            Long productId, Long warehouseId);

    // All movements for a product across all warehouses — ordered newest first
    List<StockMovement> findByProductIdOrderByMovementDateDesc(Long productId);

    // Total units received (STOCK_IN) for a product in a warehouse
    @Query("SELECT COALESCE(SUM(m.quantity), 0) FROM StockMovement m " +
           "WHERE m.productId = :productId AND m.warehouseId = :warehouseId " +
           "AND m.movementType = 'STOCK_IN'")
    int sumStockIn(@Param("productId") Long productId,
                   @Param("warehouseId") Long warehouseId);

    // Total units issued (STOCK_OUT) for a product in a warehouse
    @Query("SELECT COALESCE(SUM(m.quantity), 0) FROM StockMovement m " +
           "WHERE m.productId = :productId AND m.warehouseId = :warehouseId " +
           "AND m.movementType = 'STOCK_OUT'")
    int sumStockOut(@Param("productId") Long productId,
                    @Param("warehouseId") Long warehouseId);

    // All movements for a product within a date range — used by Report service
    @Query("SELECT m FROM StockMovement m " +
           "WHERE m.productId = :productId " +
           "AND m.movementDate BETWEEN :from AND :to " +
           "ORDER BY m.movementDate DESC")
    List<StockMovement> findByProductAndDateRange(
            @Param("productId") Long productId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    // Count movements by type for a product — used by analytics
    @Query("SELECT COUNT(m) FROM StockMovement m " +
           "WHERE m.productId = :productId AND m.movementType = :type")
    long countByProductIdAndType(
            @Param("productId") Long productId,
            @Param("type") MovementType type);
}
