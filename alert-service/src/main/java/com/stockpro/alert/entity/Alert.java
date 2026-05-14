package com.stockpro.alert.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "alerts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The user this alert is directed to
    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Severity severity = Severity.INFO;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    // Optional — points to the product that triggered this alert
    @Column(name = "related_product_id")
    private Long relatedProductId;

    // Optional — points to the warehouse involved
    @Column(name = "related_warehouse_id")
    private Long relatedWarehouseId;

    // Delivery channel: IN_APP or EMAIL
    @Builder.Default
    private String channel = "IN_APP";

    // Has the recipient opened / seen this alert
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean isRead = false;

    // Has the recipient explicitly acknowledged (dismissed) this alert
    @Column(name = "is_acknowledged", nullable = false)
    @Builder.Default
    private boolean isAcknowledged = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum AlertType {
        LOW_STOCK,
        OVERSTOCK,
        PO_PENDING,
        OVERDUE_RECEIPT,
        SYSTEM
    }

    public enum Severity {
        INFO,
        WARNING,
        CRITICAL    // CRITICAL alerts also trigger email dispatch
    }
}
