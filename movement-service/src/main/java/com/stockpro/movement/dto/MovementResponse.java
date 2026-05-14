package com.stockpro.movement.dto;

import com.stockpro.movement.entity.StockMovement.MovementType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovementResponse {

    private Long          id;
    private Long          productId;
    private String        productName;
    private String        productSku;
    private Long          warehouseId;
    private String        warehouseName;
    private Long          fromWarehouseId;
    private String        fromWarehouseName;
    private Long          toWarehouseId;
    private String        toWarehouseName;
    private MovementType  movementType;
    private int           quantity;
    private BigDecimal    unitCost;
    private Long          referenceId;
    private String        referenceType;
    private Long          performedBy;
    private String        performedByName;
    private String        notes;
    private int           balanceAfter;
    private LocalDateTime movementDate;
}
