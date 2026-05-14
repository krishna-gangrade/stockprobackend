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
public class SnapshotResponse {
    private Long       id;
    private Long       warehouseId;
    private String     warehouseName;
    private Long       productId;
    private String     productName;
    private String     productSku;
    private int        quantity;
    private BigDecimal stockValue;
    private LocalDate  snapshotDate;
}
