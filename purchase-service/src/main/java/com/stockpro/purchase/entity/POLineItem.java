package com.stockpro.purchase.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "po_line_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class POLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Back-reference to parent PurchaseOrder
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "po_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    // How many units were ordered on this PO line
    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "total_cost", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalCost;

    // How many units have been received so far (supports partial receipts)
    // receivedQty < quantity  → line still open (PARTIALLY_RECEIVED)
    // receivedQty == quantity → line fully received
    @Column(name = "received_qty", nullable = false)
    @Builder.Default
    private int receivedQty = 0;

    // Convenience helper — how many units still outstanding
    @Transient
    public int getPendingQty() {
        return quantity - receivedQty;
    }

    // Is this line fully received?
    @Transient
    public boolean isFullyReceived() {
        return receivedQty >= quantity;
    }
}
