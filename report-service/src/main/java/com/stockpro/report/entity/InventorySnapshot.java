package com.stockpro.report.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * InventorySnapshot — written once per day per product per warehouse.
 *
 * Created by SnapshotScheduler at midnight.
 * Stores quantity and stock value at that point in time.
 * Used by analytics to show trends over time without
 * replaying all stock movements.
 *
 * stockValue = quantity × product.costPrice at time of snapshot
 */
@Entity
@Table(name = "inventory_snapshots")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventorySnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private int quantity;

    // quantity × costPrice at time of snapshot
    @Column(name = "stock_value", nullable = false, precision = 14, scale = 2)
    private BigDecimal stockValue;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
