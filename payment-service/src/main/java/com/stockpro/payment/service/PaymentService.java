package com.stockpro.payment.service;

import com.stockpro.payment.dto.PaymentRequest;
import com.stockpro.payment.dto.PaymentResponse;
import com.stockpro.payment.dto.RazorpayOrderResponse;
import com.stockpro.payment.dto.SupplierReturnAdjustmentRequest;
import com.stockpro.payment.entity.Payment;

import java.util.List;

public interface PaymentService {
    RazorpayOrderResponse createPayment(PaymentRequest request, Long createdById) throws Exception;
    PaymentResponse getById(Long id);
    List<PaymentResponse> getAll();
    List<PaymentResponse> getByPurchaseOrder(Long purchaseOrderId);
    List<PaymentResponse> getBySupplier(Long supplierId);
    List<PaymentResponse> getByStatus(Payment.PaymentStatus status);
    List<PaymentResponse> getOverduePayments();
    PaymentResponse markFailed(Long id, String reason);
    void cancelPayment(Long id, String reason);
    PaymentResponse applySupplierReturn(Long purchaseOrderId, SupplierReturnAdjustmentRequest request);

    // Razorpay integration
    RazorpayOrderResponse createRazorpayOrder(Long paymentId) throws Exception;
    PaymentResponse verifyRazorpayPayment(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature, java.math.BigDecimal mockPaidAmount) throws Exception;
    List<PaymentResponse> getByDateRange(java.time.LocalDate from, java.time.LocalDate to);
}
