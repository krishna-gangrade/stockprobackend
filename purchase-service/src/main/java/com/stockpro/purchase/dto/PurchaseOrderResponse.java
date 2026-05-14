package com.stockpro.purchase.dto;

import com.stockpro.purchase.entity.PurchaseOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderResponse {
    private Long                     id;
    private Long                     supplierId;
    private String                   supplierName;
    private Long                     warehouseId;
    private String                   warehouseName;
    private Long                     createdById;
    private PurchaseOrder.POStatus   status;
    private BigDecimal               totalAmount;
    private LocalDate                orderDate;
    private LocalDate                expectedDate;
    private LocalDate                receivedDate;
    private String                   referenceNumber;
    private String                   notes;
    private LocalDateTime            createdAt;
    private LocalDateTime            updatedAt;
    private List<POLineItemResponse> lineItems;
}
