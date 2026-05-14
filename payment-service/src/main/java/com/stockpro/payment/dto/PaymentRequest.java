package com.stockpro.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PaymentRequest {

    @NotNull(message = "purchaseOrderId is required")
    private Long purchaseOrderId;

    @NotNull(message = "supplierId is required")
    private Long supplierId;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be greater than 0")
    private BigDecimal amount;

    private LocalDate dueDate;
    private String notes;
}
