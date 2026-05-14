package com.stockpro.warehouse.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "warehouses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Warehouse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String location;

    @Column(columnDefinition = "TEXT")
    private String address;

    // FK to users table — stored as plain Long (no @ManyToOne to avoid cross-package coupling)
    @Column(name = "manager_id")
    private Long managerId;

    @Column(nullable = false)
    @Builder.Default
    private int capacity = 1000;

    @Column(name = "used_capacity", nullable = false)
    @Builder.Default
    private int usedCapacity = 0;

    private String phone;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
