package com.stockpro.purchase.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class POCreatedEvent {
    private Long poId;
    private Long supplierId;
    private Long createdById;
    private BigDecimal totalAmount;
    private LocalDate dueDate;
    private String referenceNumber;
    private LocalDateTime createdAt;
}
