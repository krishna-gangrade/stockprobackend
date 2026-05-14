package com.stockpro.purchase.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class POLineItemResponse {
    private Long       id;
    private Long       productId;
    private String     productName;
    private String     productSku;
    private int        quantity;
    private BigDecimal unitCost;
    private BigDecimal totalCost;
    private int        receivedQty;
    private int        pendingQty;
    private boolean    fullyReceived;
}
