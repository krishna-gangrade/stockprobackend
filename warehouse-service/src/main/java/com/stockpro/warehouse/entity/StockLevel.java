package com.stockpro.warehouse.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "stock_levels",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_stock_warehouse_product",
        columnNames = {"warehouse_id", "product_id"}
    )
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    @Builder.Default
    private int quantity = 0;

    // Stock reserved / allocated to open orders — not yet dispatched
    @Column(name = "reserved_quantity", nullable = false)
    @Builder.Default
    private int reservedQuantity = 0;

    // Bin or aisle reference inside the warehouse, e.g. "A-12-3"
    @Column(name = "bin_location")
    private String binLocation;

    @Column(name = "last_updated")
    @Builder.Default
    private LocalDateTime lastUpdated = LocalDateTime.now();

    // Optimistic locking — prevents two threads decrementing stock simultaneously.
    // JPA increments this automatically on every UPDATE.
    // If two transactions read version=5 and both try to save, the second one
    // sees a stale version and throws OptimisticLockException → retry logic handles it.
    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;

    // ===== Computed helper — NOT stored in DB =====
    // Available = total on hand minus what's already reserved for open orders
    @Transient
    public int getAvailableQuantity() {
        return Math.max(0, quantity - reservedQuantity);
    }
}
