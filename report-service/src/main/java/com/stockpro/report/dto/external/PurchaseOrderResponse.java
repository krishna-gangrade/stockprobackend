package com.stockpro.report.dto.external;

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
public class PurchaseOrderResponse {
    private Long id;
    private String status; // DRAFT, PENDING, APPROVED, etc.
    private BigDecimal totalAmount;
    private LocalDate orderDate;
    private Long supplierId;
    private String supplierName;
}
