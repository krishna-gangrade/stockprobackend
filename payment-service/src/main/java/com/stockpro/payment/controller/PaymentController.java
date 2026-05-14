package com.stockpro.payment.controller;

import com.stockpro.payment.dto.PaymentRequest;
import com.stockpro.payment.dto.PaymentResponse;
import com.stockpro.payment.dto.RazorpayOrderResponse;
import com.stockpro.payment.dto.SupplierReturnAdjustmentRequest;
import com.stockpro.payment.entity.Payment;
import com.stockpro.payment.response.ApiResponse;
import com.stockpro.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Supplier payment lifecycle for purchase orders")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('PURCHASE_OFFICER', 'INVENTORY_MANAGER', 'ADMIN')")
    @Operation(summary = "Create a Razorpay order for a purchase order payment")
    public ResponseEntity<ApiResponse<RazorpayOrderResponse>> createPayment(
            @Valid @RequestBody PaymentRequest request,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) throws Exception {
        RazorpayOrderResponse response = paymentService.createPayment(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Razorpay order created", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payment by ID")
    public ResponseEntity<ApiResponse<PaymentResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.getById(id)));
    }

    @GetMapping
    @Operation(summary = "Get all payments")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.getAll()));
    }

    @GetMapping("/purchase-order/{purchaseOrderId}")
    @Operation(summary = "Get payments for a purchase order")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getByPurchaseOrder(@PathVariable Long purchaseOrderId) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.getByPurchaseOrder(purchaseOrderId)));
    }

    @GetMapping("/supplier/{supplierId}")
    @Operation(summary = "Get payments for a supplier")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getBySupplier(@PathVariable Long supplierId) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.getBySupplier(supplierId)));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get payments by status")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getByStatus(@PathVariable Payment.PaymentStatus status) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.getByStatus(status)));
    }

    @GetMapping("/overdue")
    @Operation(summary = "Get pending or partial payments past their due date")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getOverduePayments() {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.getOverduePayments()));
    }

    @PostMapping("/purchase-order/{purchaseOrderId}/supplier-return")
    @PreAuthorize("hasAnyRole('PURCHASE_OFFICER', 'INVENTORY_MANAGER', 'ADMIN')")
    @Operation(summary = "Apply a supplier return against the payment for a purchase order")
    public ResponseEntity<ApiResponse<PaymentResponse>> applySupplierReturn(
            @PathVariable Long purchaseOrderId,
            @Valid @RequestBody SupplierReturnAdjustmentRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Supplier return applied to payment",
                paymentService.applySupplierReturn(purchaseOrderId, request)
        ));
    }

    @PutMapping("/{id}/failed")
    @PreAuthorize("hasAnyRole('PURCHASE_OFFICER', 'INVENTORY_MANAGER', 'ADMIN')")
    @Operation(summary = "Mark payment as failed")
    public ResponseEntity<ApiResponse<PaymentResponse>> markFailed(
            @PathVariable Long id,
            @RequestParam String reason) {
        return ResponseEntity.ok(ApiResponse.ok("Payment marked failed", paymentService.markFailed(id, reason)));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('PURCHASE_OFFICER', 'INVENTORY_MANAGER', 'ADMIN')")
    @Operation(summary = "Cancel an unpaid payment")
    public ResponseEntity<ApiResponse<Void>> cancelPayment(
            @PathVariable Long id,
            @RequestParam String reason) {
        paymentService.cancelPayment(id, reason);
        return ResponseEntity.ok(ApiResponse.ok("Payment cancelled", null));
    }

    @PostMapping("/{id}/razorpay-order")
    @PreAuthorize("hasAnyRole('PURCHASE_OFFICER', 'INVENTORY_MANAGER', 'ADMIN', 'USER')")
    @Operation(summary = "Create a Razorpay order for an existing payment")
    public ResponseEntity<ApiResponse<RazorpayOrderResponse>> createRazorpayOrder(@PathVariable Long id) throws Exception {
        return ResponseEntity.ok(ApiResponse.ok("Razorpay order created", paymentService.createRazorpayOrder(id)));
    }

    @PostMapping("/razorpay-verify")
    @PreAuthorize("hasAnyRole('PURCHASE_OFFICER', 'INVENTORY_MANAGER', 'ADMIN', 'USER')")
    @Operation(summary = "Verify Razorpay payment signature and populate data")
    public ResponseEntity<ApiResponse<PaymentResponse>> verifyRazorpayPayment(
            @Valid @RequestBody com.stockpro.payment.dto.RazorpayVerifyRequest request) throws Exception {
        return ResponseEntity.ok(ApiResponse.ok("Payment verified and recorded", 
                paymentService.verifyRazorpayPayment(request.getRazorpayOrderId(), request.getRazorpayPaymentId(), request.getRazorpaySignature(), request.getMockPaidAmount())));
    }
    @GetMapping("/filter")
    @Operation(summary = "Get payments between two dates")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getByDateRange(
            @RequestParam("from") @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate from,
            @RequestParam("to") @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate to) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.getByDateRange(from, to)));
    }
}
