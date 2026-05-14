package com.stockpro.purchase.controller;

import com.stockpro.purchase.response.ApiResponse;
import com.stockpro.purchase.dto.*;
import com.stockpro.purchase.entity.PurchaseOrder;
import com.stockpro.purchase.service.PurchaseService;
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

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/purchase-orders")
@RequiredArgsConstructor
@Tag(name = "Purchase Orders", description = "Full PO lifecycle — create, approve, receive goods")
public class PurchaseController {

    private final PurchaseService purchaseService;

    // ===== CREATE =====

    @PostMapping
    @PreAuthorize("hasAnyRole('PURCHASE_OFFICER', 'ADMIN')")
    @Operation(summary = "Create a new Purchase Order [PURCHASE_OFFICER, ADMIN]")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "PO created as DRAFT")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> createPO(
            @Valid @RequestBody PurchaseOrderRequest request,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        PurchaseOrderResponse response = purchaseService.createPO(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Purchase Order created", response));
    }

    // ===== READ =====

    @GetMapping("/{id}")
    @Operation(summary = "Get Purchase Order by ID")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(purchaseService.getById(id)));
    }

    @GetMapping
    @Operation(summary = "Get all Purchase Orders")
    public ResponseEntity<ApiResponse<List<PurchaseOrderResponse>>> getAllPOs() {
        return ResponseEntity.ok(ApiResponse.ok(purchaseService.getAllPOs()));
    }

    @GetMapping("/supplier/{supplierId}")
    @Operation(summary = "Get POs by supplier")
    public ResponseEntity<ApiResponse<List<PurchaseOrderResponse>>> getBySupplier(
            @PathVariable Long supplierId) {
        return ResponseEntity.ok(ApiResponse.ok(purchaseService.getBySupplier(supplierId)));
    }

    @GetMapping("/warehouse/{warehouseId}")
    @Operation(summary = "Get POs by warehouse")
    public ResponseEntity<ApiResponse<List<PurchaseOrderResponse>>> getByWarehouse(
            @PathVariable Long warehouseId) {
        return ResponseEntity.ok(ApiResponse.ok(purchaseService.getByWarehouse(warehouseId)));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get POs by status (DRAFT / PENDING / APPROVED / RECEIVED / CANCELLED)")
    public ResponseEntity<ApiResponse<List<PurchaseOrderResponse>>> getByStatus(
            @PathVariable PurchaseOrder.POStatus status) {
        return ResponseEntity.ok(ApiResponse.ok(purchaseService.getByStatus(status)));
    }

    @GetMapping("/date-range")
    @Operation(summary = "Get POs within a date range")
    public ResponseEntity<ApiResponse<List<PurchaseOrderResponse>>> getByDateRange(
            @Parameter(description = "From date (yyyy-MM-dd)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "To date (yyyy-MM-dd)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.ok(purchaseService.getByDateRange(from, to)));
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasAnyRole('INVENTORY_MANAGER', 'ADMIN')")
    @Operation(summary = "Get approved POs past their expected delivery date [INVENTORY_MANAGER, ADMIN]")
    public ResponseEntity<ApiResponse<List<PurchaseOrderResponse>>> getOverduePOs() {
        return ResponseEntity.ok(ApiResponse.ok(purchaseService.getOverduePOs()));
    }

    // ===== UPDATE =====

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PURCHASE_OFFICER', 'ADMIN')")
    @Operation(summary = "Update a DRAFT Purchase Order [PURCHASE_OFFICER, ADMIN]")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> updatePO(
            @PathVariable Long id,
            @Valid @RequestBody PurchaseOrderRequest request) {
        return ResponseEntity.ok(
                ApiResponse.ok("Purchase Order updated", purchaseService.updatePO(id, request)));
    }

    // ===== STATUS TRANSITIONS =====

    @PutMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('PURCHASE_OFFICER', 'ADMIN')")
    @Operation(summary = "Submit PO for approval — DRAFT → PENDING [PURCHASE_OFFICER, ADMIN]")
    public ResponseEntity<ApiResponse<Void>> submitForApproval(@PathVariable Long id) {
        purchaseService.submitForApproval(id);
        return ResponseEntity.ok(ApiResponse.ok("PO submitted for approval", null));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('INVENTORY_MANAGER', 'ADMIN')")
    @Operation(summary = "Approve a PO — PENDING → APPROVED [INVENTORY_MANAGER, ADMIN] — fires Kafka event")
    public ResponseEntity<ApiResponse<Void>> approvePO(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        purchaseService.approvePO(id, userId);
        return ResponseEntity.ok(ApiResponse.ok("PO approved successfully", null));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('INVENTORY_MANAGER', 'ADMIN')")
    @Operation(summary = "Reject a PO — PENDING → DRAFT [INVENTORY_MANAGER, ADMIN] — fires Kafka event")
    public ResponseEntity<ApiResponse<Void>> rejectPO(
            @PathVariable Long id,
            @RequestParam String reason,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        purchaseService.rejectPO(id, userId, reason);
        return ResponseEntity.ok(ApiResponse.ok("PO rejected", null));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('PURCHASE_OFFICER', 'INVENTORY_MANAGER', 'ADMIN')")
    @Operation(summary = "Cancel an open PO [PURCHASE_OFFICER, MANAGER, ADMIN]")
    public ResponseEntity<ApiResponse<Void>> cancelPO(
            @PathVariable Long id,
            @RequestParam String reason) {
        purchaseService.cancelPO(id, reason);
        return ResponseEntity.ok(ApiResponse.ok("PO cancelled", null));
    }

    // ===== GOODS RECEIPT =====

    @PostMapping("/{id}/receive")
    @PreAuthorize("hasAnyRole('WAREHOUSE_STAFF', 'INVENTORY_MANAGER', 'ADMIN')")
    @Operation(summary = "Record goods receipt against an approved PO — supports partial receipt [WAREHOUSE_STAFF, MANAGER, ADMIN]")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Goods received, stock updated")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> receiveGoods(
            @PathVariable Long id,
            @Valid @RequestBody List<ReceiveGoodsRequest> receipts) {
        PurchaseOrderResponse response = purchaseService.receiveGoods(id, receipts);
        return ResponseEntity.ok(ApiResponse.ok("Goods received and stock updated", response));
    }
    
    // The userId is now extracted directly via @RequestHeader in the controller methods.
    // In a production environment, this header should be trusted ONLY if it comes from the API Gateway.
}
