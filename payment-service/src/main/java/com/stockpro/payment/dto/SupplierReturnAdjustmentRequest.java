package com.stockpro.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierReturnAdjustmentRequest {

    @NotNull(message = "Return amount is required")
    @DecimalMin(value = "0.01", message = "Return amount must be greater than zero")
    private BigDecimal amount;

    @NotNull(message = "Adjustment action is required")
    private AdjustmentAction action;

    private String reason;

    public enum AdjustmentAction {
        REDUCE_PAYABLE,
        RECORD_SUPPLIER_REFUND
    }
}
