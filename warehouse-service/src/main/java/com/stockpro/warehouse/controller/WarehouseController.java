package com.stockpro.warehouse.controller;

import com.stockpro.warehouse.response.ApiResponse;
import com.stockpro.warehouse.dto.*;
import com.stockpro.warehouse.service.WarehouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/warehouses")
@RequiredArgsConstructor
@Tag(name = "Warehouses", description = "Warehouse management and stock level operations")
public class WarehouseController {

    private final WarehouseService warehouseService;

    // =====================================================================
    // WAREHOUSE CRUD ENDPOINTS
    // =====================================================================

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new warehouse [ADMIN only]")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Warehouse created")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Warehouse name already exists")
    public ResponseEntity<ApiResponse<WarehouseResponse>> createWarehouse(
            @Valid @RequestBody WarehouseRequest request) {
        WarehouseResponse response = warehouseService.createWarehouse(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Warehouse created successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get warehouse by ID")
    public ResponseEntity<ApiResponse<WarehouseResponse>> getById(
            @Parameter(description = "Warehouse ID") @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(warehouseService.getById(id)));
    }

    @GetMapping("/exists/{id}")
    @Operation(summary = "Check if warehouse exists")
    public ResponseEntity<ApiResponse<Boolean>> existsById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(warehouseService.existsById(id)));
    }

    @GetMapping
    @Operation(summary = "Get all warehouses (active + inactive)")
    public ResponseEntity<ApiResponse<List<WarehouseResponse>>> getAllWarehouses() {
        return ResponseEntity.ok(ApiResponse.ok(warehouseService.getAllWarehouses()));
    }

    @GetMapping("/active")
    @Operation(summary = "Get all active warehouses")
    public ResponseEntity<ApiResponse<List<WarehouseResponse>>> getActiveWarehouses() {
        return ResponseEntity.ok(ApiResponse.ok(warehouseService.getActiveWarehouses()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update warehouse details [ADMIN only]")
    public ResponseEntity<ApiResponse<WarehouseResponse>> updateWarehouse(
            @PathVariable Long id,
            @Valid @RequestBody WarehouseRequest request) {
        return ResponseEntity.ok(
                ApiResponse.ok("Warehouse updated", warehouseService.updateWarehouse(id, request)));
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate a warehouse [ADMIN only]")
    public ResponseEntity<ApiResponse<Void>> deactivateWarehouse(@PathVariable Long id) {
        warehouseService.deactivateWarehouse(id);
        return ResponseEntity.ok(ApiResponse.ok("Warehouse deactivated", null));
    }

    @PutMapping("/{warehouseId}/manager/{managerId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assign a manager to a warehouse [ADMIN only]")
    public ResponseEntity<ApiResponse<Void>> assignManager(
            @PathVariable Long warehouseId,
            @PathVariable Long managerId) {
        warehouseService.assignManager(warehouseId, managerId);
        return ResponseEntity.ok(ApiResponse.ok("Manager assigned successfully", null));
    }

    // =====================================================================
    // STOCK LEVEL ENDPOINTS
    // =====================================================================

    @GetMapping("/{warehouseId}/stock")
    @Operation(summary = "Get all stock levels for a warehouse")
    public ResponseEntity<ApiResponse<List<StockLevelResponse>>> getStockByWarehouse(
            @Parameter(description = "Warehouse ID") @PathVariable Long warehouseId) {
        return ResponseEntity.ok(
                ApiResponse.ok(warehouseService.getStockByWarehouse(warehouseId)));
    }

    @GetMapping("/{warehouseId}/stock/{productId}")
    @Operation(summary = "Get stock level for a specific product in a warehouse")
    public ResponseEntity<ApiResponse<StockLevelResponse>> getStockLevel(
            @PathVariable Long warehouseId,
            @PathVariable Long productId) {
        return ResponseEntity.ok(
                ApiResponse.ok(warehouseService.getStockLevel(warehouseId, productId)));
    }

    @GetMapping("/stock/product/{productId}")
    @Operation(summary = "Get stock levels for a product across all warehouses")
    public ResponseEntity<ApiResponse<List<StockLevelResponse>>> getStockByProduct(
            @Parameter(description = "Product ID") @PathVariable Long productId) {
        return ResponseEntity.ok(
                ApiResponse.ok(warehouseService.getStockByProduct(productId)));
    }

    @PutMapping("/stock")
    @PreAuthorize("hasAnyRole('INVENTORY_MANAGER', 'ADMIN')")
    @Operation(summary = "Update stock quantity for a product in a warehouse [INVENTORY_MANAGER, ADMIN]")
    public ResponseEntity<ApiResponse<StockLevelResponse>> updateStock(
            @Valid @RequestBody StockUpdateRequest request) {
        return ResponseEntity.ok(
                ApiResponse.ok("Stock updated", warehouseService.updateStock(request)));
    }

    // =====================================================================
    // RESERVE & RELEASE ENDPOINTS (used internally by PO service)
    // =====================================================================

    @PostMapping("/stock/reserve")
    @PreAuthorize("hasAnyRole('PURCHASE_OFFICER', 'INVENTORY_MANAGER', 'ADMIN')")
    @Operation(summary = "Reserve stock for a purchase order [PURCHASE_OFFICER, MANAGER, ADMIN]")
    public ResponseEntity<ApiResponse<Void>> reserveStock(
            @RequestParam Long warehouseId,
            @RequestParam Long productId,
            @RequestParam int quantity) {
        warehouseService.reserveStock(warehouseId, productId, quantity);
        return ResponseEntity.ok(ApiResponse.ok("Stock reserved successfully", null));
    }

    @PostMapping("/stock/release")
    @PreAuthorize("hasAnyRole('PURCHASE_OFFICER', 'INVENTORY_MANAGER', 'ADMIN')")
    @Operation(summary = "Release reserved stock [PURCHASE_OFFICER, MANAGER, ADMIN]")
    public ResponseEntity<ApiResponse<Void>> releaseStock(
            @RequestParam Long warehouseId,
            @RequestParam Long productId,
            @RequestParam int quantity) {
        warehouseService.releaseReservation(warehouseId, productId, quantity);
        return ResponseEntity.ok(ApiResponse.ok("Reservation released", null));
    }

    // =====================================================================
    // TRANSFER ENDPOINT
    // =====================================================================

    @PostMapping("/stock/transfer")
    @PreAuthorize("hasAnyRole('WAREHOUSE_STAFF', 'INVENTORY_MANAGER', 'ADMIN')")
    @Operation(summary = "Transfer stock between warehouses [WAREHOUSE_STAFF, MANAGER, ADMIN]")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Transfer completed")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Insufficient stock or same warehouse")
    public ResponseEntity<ApiResponse<Void>> transferStock(
            @Valid @RequestBody TransferRequest request) {
        warehouseService.transferStock(request);
        return ResponseEntity.ok(ApiResponse.ok(
                "Stock transferred successfully from warehouse "
                + request.getFromWarehouseId() + " to " + request.getToWarehouseId(), null));
    }

    // =====================================================================
    // LOW STOCK & OVERSTOCK ENDPOINTS
    // =====================================================================

    @GetMapping("/stock/low-stock")
    @PreAuthorize("hasAnyRole('INVENTORY_MANAGER', 'ADMIN')")
    @Operation(summary = "Get all products below their reorder level [INVENTORY_MANAGER, ADMIN]")
    public ResponseEntity<ApiResponse<List<StockLevelResponse>>> getLowStockItems() {
        return ResponseEntity.ok(ApiResponse.ok(warehouseService.getLowStockItems()));
    }

    @GetMapping("/stock/overstock")
    @PreAuthorize("hasAnyRole('INVENTORY_MANAGER', 'ADMIN')")
    @Operation(summary = "Get all products exceeding their max stock level [INVENTORY_MANAGER, ADMIN]")
    public ResponseEntity<ApiResponse<List<StockLevelResponse>>> getOverstockItems() {
        return ResponseEntity.ok(ApiResponse.ok(warehouseService.getOverstockItems()));
    }
}
