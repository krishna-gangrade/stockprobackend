package com.stockpro.movement.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * LowStockEvent — Kafka message payload published to "low-stock-events" topic.
 *
 * Published by: the low-stock scheduler in Warehouse service (every 15 minutes).
 * Consumed by: LowStockEventConsumer in this Movement service.
 *
 * On receipt the consumer creates a LOW_STOCK alert via the Alert service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LowStockEvent {

    private Long          productId;
    private String        productName;
    private String        productSku;
    private Long          warehouseId;
    private String        warehouseName;
    private int           currentQuantity;
    private int           reorderLevel;
    private int           availableQuantity;
    private LocalDateTime eventTime;
}
