package com.stockpro.report.service;

import com.stockpro.report.dto.*;

import java.time.LocalDate;
import java.util.List;

public interface ReportService {

    // ===== Snapshot =====
    void                         takeSnapshot();
    List<SnapshotResponse>       getSnapshotByDate(LocalDate date);
    List<SnapshotResponse>       getSnapshotByWarehouse(Long warehouseId);

    // ===== Valuation =====
    StockValuationResponse       getTotalStockValue();
    StockValuationResponse       getStockValueByWarehouse(Long warehouseId);

    // ===== Turnover =====
    TurnoverResponse             getInventoryTurnover(LocalDate from, LocalDate to);
    TurnoverResponse             getInventoryTurnoverByWarehouse(
                                         Long warehouseId, LocalDate from, LocalDate to);

    // ===== Product Movement Analytics =====
    List<TopMovingProductResponse> getTopMovingProducts(int topN);
    List<TopMovingProductResponse> getSlowMovingProducts(int thresholdDays);
    List<DeadStockResponse>        getDeadStock(int deadStockDays);

    // ===== PO Summary =====
    POSummaryResponse            getPOSummary(LocalDate from, LocalDate to);
    POSummaryResponse            getPOSummaryBySupplier(Long supplierId,
                                         LocalDate from, LocalDate to);
}
