package com.stockpro.purchase.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Kafka event published to topic "po-approval-events" when a PO is approved.
 *
 * Consumers of this event:
 *   - Alert Service: sends PO approval notification to the purchase officer
 *   - (Future) ERP integration: syncs approved PO to external systems
 *
 * Why Kafka here instead of a direct service call?
 * → The alert service is logically separate. If the alert service is down,
 *   the PO approval should still succeed. Kafka decouples the two operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class POApprovalEvent {

    private Long          poId;
    private String        referenceNumber;
    private Long          supplierId;
    private String        supplierName;
    private Long          warehouseId;
    private Long          approvedById;
    private BigDecimal    totalAmount;
    private String        status;          // "APPROVED" or "REJECTED"
    private String        reason;          // populated when rejected
    private LocalDateTime eventTime;
}
