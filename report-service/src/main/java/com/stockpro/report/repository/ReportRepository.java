package com.stockpro.report.repository;

import com.stockpro.report.entity.InventorySnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<InventorySnapshot, Long> {

    List<InventorySnapshot> findBySnapshotDate(LocalDate date);

    List<InventorySnapshot> findByWarehouseId(Long warehouseId);

    List<InventorySnapshot> findByProductId(Long productId);

    List<InventorySnapshot> findBySnapshotDateBetween(LocalDate from, LocalDate to);

    Optional<InventorySnapshot> findByWarehouseIdAndProductIdAndSnapshotDate(
            Long warehouseId, Long productId, LocalDate date);

    // Total stock value across ALL warehouses on the latest snapshot date
    @Query("SELECT COALESCE(SUM(s.stockValue), 0) FROM InventorySnapshot s " +
           "WHERE s.snapshotDate = :date")
    BigDecimal sumTotalStockValueByDate(@Param("date") LocalDate date);

    // Total stock value for a specific warehouse on a given date
    @Query("SELECT COALESCE(SUM(s.stockValue), 0) FROM InventorySnapshot s " +
           "WHERE s.warehouseId = :warehouseId AND s.snapshotDate = :date")
    BigDecimal sumStockValueByWarehouseAndDate(
            @Param("warehouseId") Long warehouseId,
            @Param("date") LocalDate date);

    // Average stock value for a product across a date range — used for turnover calc
    @Query("SELECT COALESCE(AVG(s.stockValue), 0) FROM InventorySnapshot s " +
           "WHERE s.productId = :productId " +
           "AND s.snapshotDate BETWEEN :from AND :to")
    BigDecimal avgStockValueByProductAndDateRange(
            @Param("productId") Long productId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    // Most recent snapshot date recorded
    @Query("SELECT MAX(s.snapshotDate) FROM InventorySnapshot s")
    Optional<LocalDate> findLatestSnapshotDate();

    // Products with no snapshot in the last N days — dead stock candidates
    @Query("SELECT DISTINCT s.productId FROM InventorySnapshot s " +
           "WHERE s.snapshotDate = :latestDate AND s.quantity > 0 " +
           "AND s.productId NOT IN (" +
           "  SELECT DISTINCT s2.productId FROM InventorySnapshot s2 " +
           "  WHERE s2.snapshotDate >= :cutoffDate AND s2.quantity != s2.quantity" +
           ")")
    List<Long> findDeadStockProductIds(
            @Param("latestDate") LocalDate latestDate,
            @Param("cutoffDate") LocalDate cutoffDate);
}
