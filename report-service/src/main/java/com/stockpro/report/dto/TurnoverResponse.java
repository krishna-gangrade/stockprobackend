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
public class TurnoverResponse {
    // turnoverRate = COGS / averageInventoryValue
    // COGS = total cost of all STOCK_OUT movements in the period
    private BigDecimal turnoverRate;
    private BigDecimal totalCOGS;
    private BigDecimal averageInventoryValue;
    private LocalDate  fromDate;
    private LocalDate  toDate;
    private Long       warehouseId;   // null means all warehouses
}
