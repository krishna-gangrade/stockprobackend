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
public class StockValuationResponse {
    private BigDecimal totalValue;
    private LocalDate  asOfDate;
    private Long       warehouseId;    // null means all warehouses
    private String     warehouseName;  // null means all warehouses
    private int        totalProducts;
}
