package com.stockpro.warehouse.dto.external;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertRequest {
    private Long recipientId;
    private AlertType type;
    private Severity severity;
    private String title;
    private String message;
    private Long relatedProductId;
    private Long relatedWarehouseId;
    private String channel;

    public enum AlertType {
        LOW_STOCK, OVERSTOCK, PO_PENDING, PO_APPROVED, PAYMENT_RECEIVED, SYSTEM
    }

    public enum Severity {
        INFO, WARNING, CRITICAL
    }
}
