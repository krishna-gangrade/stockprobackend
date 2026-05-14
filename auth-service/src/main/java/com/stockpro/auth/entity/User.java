package com.stockpro.auth.entity;

// Entity refreshed
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * User — the core identity entity for all StockPro platform users.
 * Optimistic locking via @Version prevents concurrent update conflicts.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "passwordHash")
@EqualsAndHashCode(of = "userId")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "phone", length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "role", nullable = false)
    private Role role;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public enum Role {
        /** Day-to-day operator: stock receipts, issues, transfers, adjustments */
        WAREHOUSE_STAFF,
        /** Oversees products, stock health, reports, alert config, warehouse setup */
        INVENTORY_MANAGER,
        /** Manages procurement: purchase orders and supplier relationships */
        PURCHASE_OFFICER,
        /** System administrator: users, warehouses, system config, analytics */
        ADMIN,
        /** Automated component: triggers low-stock / overstock alerts and PO reminders */
        SYSTEM,
        /** External supplier: fulfils purchase orders and receives reorder requests */
        SUPPLIER
    }
}
