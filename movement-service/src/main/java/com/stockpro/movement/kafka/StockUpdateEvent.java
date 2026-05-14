package com.stockpro.movement.kafka;

import com.stockpro.movement.entity.StockMovement.MovementType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockUpdateEvent {
    private Long productId;
    private Long warehouseId;
    private int quantity;
    private MovementType movementType;
    private Long movementId;
}
