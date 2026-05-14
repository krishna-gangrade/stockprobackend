package com.stockpro.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeadStockResponse {
    private Long       productId;
    private String     productName;
    private String     productSku;
    private String     category;
    private int        currentQuantity;
    private BigDecimal stockValue;
    private LocalDate  lastMovementDate;  // null if never moved
    private int        daysSinceLastMovement;
}
