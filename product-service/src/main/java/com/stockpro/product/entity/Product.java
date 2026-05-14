package com.stockpro.product.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(nullable = false)
    private String name;

    private String description;

    private String category;
    
    private String brand;

    @Column(nullable = false)
    private BigDecimal costPrice;

    @Column
    private BigDecimal sellingPrice;

    @Column(nullable = false)
    @Builder.Default
    private int reorderLevel = 10;

    @Column(nullable = false)
    @Builder.Default
    private int maxStockLevel = 1000;

    @Column(nullable = false)
    @Builder.Default
    private int leadTimeDays = 7;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(nullable = false)
    private String unit; // e.g., PCS, KG, L

    private String imageUrl;
}
