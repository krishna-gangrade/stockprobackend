package com.stockpro.warehouse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockLevelResponse {

    private Long          id;
    private Long          warehouseId;
    private String        warehouseName;   // populated from Warehouse lookup
    private Long          productId;
    private String        productName;     // populated from Product lookup
    private String        productSku;
    private int           quantity;
    private int           reservedQuantity;
    private int           availableQuantity;  // quantity - reservedQuantity
    private String        binLocation;
    private LocalDateTime lastUpdated;
}
