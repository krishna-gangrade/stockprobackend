package com.stockpro.purchase.dto.external;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockUpdateRequest {
    private Long warehouseId;
    private Long productId;
    private int quantity; // new total quantity OR increment? In WarehouseService it sets the quantity.
    private String binLocation;
}
