package com.stockpro.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopMovingProductResponse {
    private Long       productId;
    private String     productName;
    private String     productSku;
    private String     category;
    private int        totalUnitsIn;
    private int        totalUnitsOut;
    private int        totalUnitsMoved;   // in + out
    private BigDecimal totalValueMoved;
    private int        rank;
}
