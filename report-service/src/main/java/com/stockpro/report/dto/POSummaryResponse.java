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
public class POSummaryResponse {
    private int        totalPOs;
    private int        approvedPOs;
    private int        pendingPOs;
    private int        cancelledPOs;
    private BigDecimal totalSpend;
    private LocalDate  fromDate;
    private LocalDate  toDate;
    private Long       supplierId;    // null means all suppliers
    private String     supplierName;
    private Long       warehouseId;  // null means all warehouses
}
