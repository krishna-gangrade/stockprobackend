package com.stockpro.movement.dto.external;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequest {
    private Long productId;
    private Long fromWarehouseId;
    private Long toWarehouseId;
    private int quantity;
    private String reason;
}
