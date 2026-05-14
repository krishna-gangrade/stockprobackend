package com.stockpro.movement.dto;

import com.stockpro.movement.entity.StockMovement.MovementType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovementRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Warehouse ID is required")
    private Long warehouseId;

    private Long fromWarehouseId;

    private Long toWarehouseId;

    @NotNull(message = "Movement type is required")
    private MovementType movementType;

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;

    private BigDecimal unitCost;

    // ID of the source document (PO id, transfer id, etc.)
    private Long   referenceId;
    private String referenceType;

    private String notes;
}
