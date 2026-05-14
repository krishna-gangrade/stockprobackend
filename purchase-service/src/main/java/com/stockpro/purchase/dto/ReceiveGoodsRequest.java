package com.stockpro.purchase.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiveGoodsRequest {

    @NotNull(message = "Line item ID is required")
    private Long lineItemId;

    @Min(value = 1, message = "Received quantity must be at least 1")
    private int receivedQuantity;
}
