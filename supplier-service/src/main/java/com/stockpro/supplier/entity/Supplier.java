package com.stockpro.supplier.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "suppliers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "contact_person")
    private String contactPerson;

    @Column(nullable = false)
    private String email;

    private String phone;

    @Column(columnDefinition = "TEXT")
    private String address;

    private String city;

    private String country;

    // Payment terms e.g. NET-30, NET-60, IMMEDIATE
    @Column(name = "payment_terms")
    @Builder.Default
    private String paymentTerms = "NET-30";

    // Average number of days from PO to delivery
    @Column(name = "lead_time_days")
    @Builder.Default
    private int leadTimeDays = 7;

    // Rolling average rating (0.00 to 5.00)
    // Updated each time goods are received from this supplier
    @Column
    @Builder.Default
    private double rating = 0.00;

    // Total number of ratings received — needed to compute rolling average
    @Column(name = "rating_count")
    @Builder.Default
    private int ratingCount = 0;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
