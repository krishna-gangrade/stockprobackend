package com.stockpro.movement.controller;

import com.stockpro.movement.response.ApiResponse;
import com.stockpro.movement.dto.MovementRequest;
import com.stockpro.movement.dto.MovementResponse;
import com.stockpro.movement.entity.StockMovement.MovementType;
import com.stockpro.movement.service.MovementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/movements")
@RequiredArgsConstructor
@Tag(name = "Stock Movements", description = "Immutable audit trail of every stock change")
public class MovementController {

    private final MovementService movementService;

    // ===== RECORD (only write operation) =====

    @PostMapping
    @PreAuthorize("hasAnyRole('WAREHOUSE_STAFF', 'INVENTORY_MANAGER', 'ADMIN')")
    @Operation(summary = "Record a stock movement [STAFF, MANAGER, ADMIN]",
               description = "Movements are immutable — corrections require a new opposing movement")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Movement recorded")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Would result in negative stock")
    public ResponseEntity<ApiResponse<MovementResponse>> recordMovement(
            @Valid @RequestBody MovementRequest request,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        MovementResponse response = movementService.recordMovement(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Movement recorded successfully", response));
    }

    // ===== READ — single =====

    @GetMapping("/{id}")
    @Operation(summary = "Get movement by ID")
    public ResponseEntity<ApiResponse<MovementResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(movementService.getById(id)));
    }

    // ===== READ — lists =====

    @GetMapping
    @PreAuthorize("hasAnyRole('INVENTORY_MANAGER', 'ADMIN')")
    @Operation(summary = "Get all stock movements [MANAGER, ADMIN]")
    public ResponseEntity<ApiResponse<List<MovementResponse>>> getAllMovements() {
        return ResponseEntity.ok(ApiResponse.ok(movementService.getAllMovements()));
    }

    @GetMapping("/product/{productId}")
    @Operation(summary = "Get all movements for a product (newest first)")
    public ResponseEntity<ApiResponse<List<MovementResponse>>> getByProduct(
            @Parameter(description = "Product ID") @PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.ok(movementService.getByProduct(productId)));
    }

    @GetMapping("/warehouse/{warehouseId}")
    @Operation(summary = "Get all movements for a warehouse")
    public ResponseEntity<ApiResponse<List<MovementResponse>>> getByWarehouse(
            @Parameter(description = "Warehouse ID") @PathVariable Long warehouseId) {
        return ResponseEntity.ok(ApiResponse.ok(movementService.getByWarehouse(warehouseId)));
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "Get movements by type (STOCK_IN / STOCK_OUT / TRANSFER_IN etc.)")
    public ResponseEntity<ApiResponse<List<MovementResponse>>> getByType(
            @PathVariable MovementType type) {
        return ResponseEntity.ok(ApiResponse.ok(movementService.getByType(type)));
    }

    @GetMapping("/date-range")
    @Operation(summary = "Get movements within a date-time range")
    public ResponseEntity<ApiResponse<List<MovementResponse>>> getByDateRange(
            @Parameter(description = "From datetime (ISO format)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "To datetime (ISO format)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(ApiResponse.ok(movementService.getByDateRange(from, to)));
    }

    @GetMapping("/reference/{referenceId}")
    @Operation(summary = "Get movements by reference document ID (e.g. PO id)")
    public ResponseEntity<ApiResponse<List<MovementResponse>>> getByReference(
            @PathVariable Long referenceId) {
        return ResponseEntity.ok(ApiResponse.ok(movementService.getByReference(referenceId)));
    }

    @GetMapping("/history")
    @Operation(summary = "Get chronological movement history for a product in a warehouse")
    public ResponseEntity<ApiResponse<List<MovementResponse>>> getMovementHistory(
            @RequestParam Long productId,
            @RequestParam Long warehouseId) {
        return ResponseEntity.ok(
                ApiResponse.ok(movementService.getMovementHistory(productId, warehouseId)));
    }

    // ===== AGGREGATES =====

    @GetMapping("/stock-in/total")
    @Operation(summary = "Get total units received (STOCK_IN) for a product in a warehouse")
    public ResponseEntity<ApiResponse<Integer>> getTotalStockIn(
            @RequestParam Long productId,
            @RequestParam Long warehouseId) {
        return ResponseEntity.ok(
                ApiResponse.ok(movementService.getTotalStockIn(productId, warehouseId)));
    }

    @GetMapping("/stock-out/total")
    @Operation(summary = "Get total units issued (STOCK_OUT) for a product in a warehouse")
    public ResponseEntity<ApiResponse<Integer>> getTotalStockOut(
            @RequestParam Long productId,
            @RequestParam Long warehouseId) {
        return ResponseEntity.ok(
                ApiResponse.ok(movementService.getTotalStockOut(productId, warehouseId)));
    }
}
