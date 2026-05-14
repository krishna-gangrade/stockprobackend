package com.stockpro.report.dto.external;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private Long id;
    private String sku;
    private String name;
    private String description;
    private String category;
    private BigDecimal costPrice;
    private int reorderLevel;
    private int maxStockLevel;
    private boolean isActive;
    private String unit;
}
