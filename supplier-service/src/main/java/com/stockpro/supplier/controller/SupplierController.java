package com.stockpro.supplier.controller;

import com.stockpro.supplier.response.ApiResponse;
import com.stockpro.supplier.dto.SupplierRatingRequest;
import com.stockpro.supplier.dto.SupplierRequest;
import com.stockpro.supplier.dto.SupplierResponse;
import com.stockpro.supplier.service.SupplierService;
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
@RequestMapping("/suppliers")
@RequiredArgsConstructor
@Tag(name = "Suppliers", description = "Supplier profile management, search, and performance rating")
public class SupplierController {

    private final SupplierService supplierService;

    // ===== CREATE =====

    @PostMapping
    @PreAuthorize("hasAnyRole('PURCHASE_OFFICER', 'ADMIN')")
    @Operation(summary = "Create a new supplier profile [OFFICER, ADMIN]")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Supplier created")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Email already exists")
    public ResponseEntity<ApiResponse<SupplierResponse>> createSupplier(
            @Valid @RequestBody SupplierRequest request) {
        SupplierResponse response = supplierService.createSupplier(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Supplier created successfully", response));
    }

    // ===== READ =====

    @GetMapping("/{id}")
    @Operation(summary = "Get supplier by ID")
    public ResponseEntity<ApiResponse<SupplierResponse>> getById(
            @Parameter(description = "Supplier ID") @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(supplierService.getById(id)));
    }

    @GetMapping
    @Operation(summary = "Get all suppliers (active + inactive)")
    public ResponseEntity<ApiResponse<List<SupplierResponse>>> getAllSuppliers() {
        return ResponseEntity.ok(ApiResponse.ok(supplierService.getAllSuppliers()));
    }

    @GetMapping("/active")
    @Operation(summary = "Get all active suppliers")
    public ResponseEntity<ApiResponse<List<SupplierResponse>>> getActiveSuppliers() {
        return ResponseEntity.ok(ApiResponse.ok(supplierService.getActiveSuppliers()));
    }

    @GetMapping("/search")
    @Operation(summary = "Search suppliers by name or contact person")
    public ResponseEntity<ApiResponse<List<SupplierResponse>>> search(
            @Parameter(description = "Search keyword") @RequestParam String keyword) {
        return ResponseEntity.ok(ApiResponse.ok(supplierService.searchSuppliers(keyword)));
    }

    @GetMapping("/city/{city}")
    @Operation(summary = "Get suppliers by city")
    public ResponseEntity<ApiResponse<List<SupplierResponse>>> getByCity(
            @PathVariable String city) {
        return ResponseEntity.ok(ApiResponse.ok(supplierService.getByCity(city)));
    }

    @GetMapping("/country/{country}")
    @Operation(summary = "Get suppliers by country")
    public ResponseEntity<ApiResponse<List<SupplierResponse>>> getByCountry(
            @PathVariable String country) {
        return ResponseEntity.ok(ApiResponse.ok(supplierService.getByCountry(country)));
    }

    @GetMapping("/top-rated")
    @Operation(summary = "Get active suppliers sorted by rating (highest first)")
    public ResponseEntity<ApiResponse<List<SupplierResponse>>> getTopRated() {
        return ResponseEntity.ok(ApiResponse.ok(supplierService.getTopRatedSuppliers()));
    }

    // ===== UPDATE =====

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PURCHASE_OFFICER', 'ADMIN')")
    @Operation(summary = "Update supplier profile [OFFICER, ADMIN]")
    public ResponseEntity<ApiResponse<SupplierResponse>> updateSupplier(
            @PathVariable Long id,
            @Valid @RequestBody SupplierRequest request) {
        return ResponseEntity.ok(
                ApiResponse.ok("Supplier updated", supplierService.updateSupplier(id, request)));
    }

    @PutMapping("/{id}/rating")
    @PreAuthorize("hasAnyRole('PURCHASE_OFFICER', 'INVENTORY_MANAGER', 'ADMIN')")
    @Operation(summary = "Rate supplier performance after goods receipt [OFFICER, MANAGER, ADMIN]")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Rating updated — rolling average recalculated")
    public ResponseEntity<ApiResponse<SupplierResponse>> updateRating(
            @PathVariable Long id,
            @Valid @RequestBody SupplierRatingRequest request) {
        return ResponseEntity.ok(
                ApiResponse.ok("Supplier rating updated", supplierService.updateRating(id, request)));
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('PURCHASE_OFFICER', 'ADMIN')")
    @Operation(summary = "Deactivate supplier — prevents new POs being raised [OFFICER, ADMIN]")
    public ResponseEntity<ApiResponse<Void>> deactivateSupplier(@PathVariable Long id) {
        supplierService.deactivateSupplier(id);
        return ResponseEntity.ok(ApiResponse.ok("Supplier deactivated", null));
    }

    // ===== DELETE (ADMIN only — hard delete) =====

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Permanently delete a supplier [ADMIN only]")
    public ResponseEntity<ApiResponse<Void>> deleteSupplier(@PathVariable Long id) {
        supplierService.deleteSupplier(id);
        return ResponseEntity.ok(ApiResponse.ok("Supplier deleted", null));
    }
}
