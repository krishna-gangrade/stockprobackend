package com.stockpro.payment.dto;

import com.stockpro.payment.entity.Payment;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponse {
    private Long id;
    private Long purchaseOrderId;
    private Long supplierId;
    private BigDecimal amount;
    private BigDecimal paidAmount;
    private BigDecimal balanceAmount;
    private Payment.PaymentStatus status;
    private Payment.PaymentMethod paymentMethod;
    private String transactionReference;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private LocalDate paymentDate;
    private LocalDate dueDate;
    private String notes;
    private Long createdById;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
