package com.stockpro.movement.dto.external;

import lombok.Data;

@Data
public class StockLevelResponse {
    private Long warehouseId;
    private Long productId;
    private int quantity;
}
