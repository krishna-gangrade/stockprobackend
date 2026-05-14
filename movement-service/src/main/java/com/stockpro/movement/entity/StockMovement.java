package com.stockpro.movement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * StockMovement — immutable record of every stock change in the system.
 *
 * Design rules (from case study):
 *  - Movements are WRITE-ONCE. No update or delete operations exist.
 *  - Corrections are made by creating a new opposing movement (e.g. negative ADJUSTMENT).
 *  - balanceAfter is stored so point-in-time stock can be reconstructed without replaying all movements.
 */
@Entity
@Table(name = "stock_movements")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "from_warehouse_id")
    private Long fromWarehouseId;

    @Column(name = "to_warehouse_id")
    private Long toWarehouseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false)
    private MovementType movementType;

    // Positive for inbound, negative for outbound
    @Column(nullable = false)
    private int quantity;

    // Cost at time of movement — important for valuation
    @Column(name = "unit_cost", precision = 12, scale = 2)
    private BigDecimal unitCost;

    // FK to the source document — PO id, issue order id, transfer id
    @Column(name = "reference_id")
    private Long referenceId;

    // What kind of document the referenceId points to: "PURCHASE_ORDER", "TRANSFER", etc.
    @Column(name = "reference_type")
    private String referenceType;

    // User who performed this movement
    @Column(name = "performed_by", nullable = false)
    private Long performedBy;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // Stock on hand AFTER this movement was applied
    // Stored so we can reconstruct historical stock without replaying all movements
    @Column(name = "balance_after", nullable = false)
    private int balanceAfter;

    // Movements are immutable — createdAt is set once and never changes
    @Column(name = "movement_date", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime movementDate = LocalDateTime.now();

    public enum MovementType {
        STOCK_IN,       // Goods received from supplier (GRN)
        STOCK_OUT,      // Stock issued / consumed / sold
        TRANSFER_IN,    // Received from another warehouse
        TRANSFER_OUT,   // Sent to another warehouse
        ADJUSTMENT,     // Manual correction (cycle count discrepancy)
        WRITE_OFF,      // Damaged, expired, or lost stock
        RETURN          // Returned from customer or to supplier
    }
}
