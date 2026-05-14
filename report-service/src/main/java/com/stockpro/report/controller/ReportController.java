package com.stockpro.report.controller;

import com.stockpro.report.response.ApiResponse;
import com.stockpro.report.dto.*;
import com.stockpro.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@Tag(name = "Reports & Analytics", description = "Inventory valuation, turnover, dead stock, and PO analytics")
public class ReportController {

    private final ReportService reportService;

    // ===== SNAPSHOT =====

    @PostMapping("/snapshot")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Manually trigger a daily inventory snapshot [ADMIN only]",
               description = "Normally runs automatically at midnight. Use this to force a snapshot.")
    public ResponseEntity<ApiResponse<Void>> takeSnapshot() {
        reportService.takeSnapshot();
        return ResponseEntity.ok(ApiResponse.ok("Snapshot taken successfully", null));
    }

    @GetMapping("/snapshot")
    @PreAuthorize("hasAnyRole('INVENTORY_MANAGER', 'ADMIN')")
    @Operation(summary = "Get inventory snapshot for a specific date [MANAGER, ADMIN]")
    public ResponseEntity<ApiResponse<List<SnapshotResponse>>> getSnapshotByDate(
            @Parameter(description = "Date (yyyy-MM-dd)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getSnapshotByDate(date)));
    }

    @GetMapping("/snapshot/warehouse/{warehouseId}")
    @PreAuthorize("hasAnyRole('INVENTORY_MANAGER', 'ADMIN')")
    @Operation(summary = "Get all snapshots for a warehouse [MANAGER, ADMIN]")
    public ResponseEntity<ApiResponse<List<SnapshotResponse>>> getSnapshotByWarehouse(
            @PathVariable Long warehouseId) {
        return ResponseEntity.ok(
                ApiResponse.ok(reportService.getSnapshotByWarehouse(warehouseId)));
    }

    // ===== VALUATION =====

    @GetMapping("/valuation/total")
    @PreAuthorize("hasAnyRole('INVENTORY_MANAGER', 'ADMIN')")
    @Operation(summary = "Get total stock value across all warehouses [MANAGER, ADMIN]",
               description = "Live calculation: SUM(quantity × costPrice) for all active stock")
    public ResponseEntity<ApiResponse<StockValuationResponse>> getTotalStockValue() {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getTotalStockValue()));
    }

    @GetMapping("/valuation/warehouse/{warehouseId}")
    @PreAuthorize("hasAnyRole('INVENTORY_MANAGER', 'ADMIN')")
    @Operation(summary = "Get stock value for a specific warehouse [MANAGER, ADMIN]")
    public ResponseEntity<ApiResponse<StockValuationResponse>> getStockValueByWarehouse(
            @PathVariable Long warehouseId) {
        return ResponseEntity.ok(
                ApiResponse.ok(reportService.getStockValueByWarehouse(warehouseId)));
    }

    // ===== TURNOVER =====

    @GetMapping("/turnover")
    @PreAuthorize("hasAnyRole('INVENTORY_MANAGER', 'ADMIN')")
    @Operation(summary = "Get inventory turnover rate for a date range [MANAGER, ADMIN]",
               description = "Turnover = COGS / Average Inventory Value. Higher = faster-moving stock.")
    public ResponseEntity<ApiResponse<TurnoverResponse>> getInventoryTurnover(
            @Parameter(description = "From date (yyyy-MM-dd)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "To date (yyyy-MM-dd)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getInventoryTurnover(from, to)));
    }

    @GetMapping("/turnover/warehouse/{warehouseId}")
    @PreAuthorize("hasAnyRole('INVENTORY_MANAGER', 'ADMIN')")
    @Operation(summary = "Get inventory turnover for a specific warehouse [MANAGER, ADMIN]")
    public ResponseEntity<ApiResponse<TurnoverResponse>> getTurnoverByWarehouse(
            @PathVariable Long warehouseId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.ok(
                reportService.getInventoryTurnoverByWarehouse(warehouseId, from, to)));
    }

    // ===== PRODUCT MOVEMENT ANALYTICS =====

    @GetMapping("/products/top-moving")
    @PreAuthorize("hasAnyRole('INVENTORY_MANAGER', 'ADMIN')")
    @Operation(summary = "Get top moving products ranked by total units moved [MANAGER, ADMIN]")
    public ResponseEntity<ApiResponse<List<TopMovingProductResponse>>> getTopMovingProducts(
            @Parameter(description = "Number of top products to return (default 10)")
            @RequestParam(defaultValue = "10") int topN) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getTopMovingProducts(topN)));
    }

    @GetMapping("/products/slow-moving")
    @PreAuthorize("hasAnyRole('INVENTORY_MANAGER', 'ADMIN')")
    @Operation(summary = "Get products with no movement in the last N days [MANAGER, ADMIN]")
    public ResponseEntity<ApiResponse<List<TopMovingProductResponse>>> getSlowMovingProducts(
            @Parameter(description = "Days threshold for slow-moving (default 30)")
            @RequestParam(defaultValue = "30") int thresholdDays) {
        return ResponseEntity.ok(
                ApiResponse.ok(reportService.getSlowMovingProducts(thresholdDays)));
    }

    @GetMapping("/products/dead-stock")
    @PreAuthorize("hasAnyRole('INVENTORY_MANAGER', 'ADMIN')")
    @Operation(summary = "Get dead stock — products with no movement for N+ days [MANAGER, ADMIN]",
               description = "Default threshold is 90 days as per case study specification.")
    public ResponseEntity<ApiResponse<List<DeadStockResponse>>> getDeadStock(
            @Parameter(description = "Days with no movement to qualify as dead stock (default 90)")
            @RequestParam(defaultValue = "90") int days) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getDeadStock(days)));
    }

    // ===== PO SUMMARY =====

    @GetMapping("/po-summary")
    @PreAuthorize("hasAnyRole('INVENTORY_MANAGER', 'PURCHASE_OFFICER', 'ADMIN')")
    @Operation(summary = "Get PO spend summary for a date range [MANAGER, OFFICER, ADMIN]")
    public ResponseEntity<ApiResponse<POSummaryResponse>> getPOSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getPOSummary(from, to)));
    }

    @GetMapping("/po-summary/supplier/{supplierId}")
    @PreAuthorize("hasAnyRole('INVENTORY_MANAGER', 'PURCHASE_OFFICER', 'ADMIN')")
    @Operation(summary = "Get PO summary for a specific supplier [MANAGER, OFFICER, ADMIN]")
    public ResponseEntity<ApiResponse<POSummaryResponse>> getPOSummaryBySupplier(
            @PathVariable Long supplierId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.ok(
                reportService.getPOSummaryBySupplier(supplierId, from, to)));
    }
}
