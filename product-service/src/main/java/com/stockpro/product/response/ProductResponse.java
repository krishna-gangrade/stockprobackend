package com.stockpro.product.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ProductResponse {
    private Long id;
    private String name;
    private String description;
    private String sku;
    private String category;
    private String brand;
    private BigDecimal costPrice;
    private BigDecimal sellingPrice;
    private String unitOfMeasure;
    private int reorderLevel;
    private int maxStockLevel;
    private int leadTimeDays;
    private boolean active;
    private String imageUrl;
}
