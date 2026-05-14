package com.stockpro.movement.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Scheduled job — runs every 15 minutes.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LowStockScheduler {
    // Fields removed to avoid unused warnings. 
    // They can be added back when implementing the logic.

    @Scheduled(fixedDelay = 900000, initialDelay = 60000)
    public void checkAndPublishLowStockEvents() {
        log.info("Low-stock scheduler (stub) running at {}", LocalDateTime.now());

        // NOTE: Direct repository access to other services' tables is prohibited.
        // This scheduler should likely be moved to the warehouse-service, which owns the StockLevel data.
        // Alternatively, it could query the warehouse-service via Feign.
        
        log.info("TODO: Implement cross-service low-stock check or move this scheduler to warehouse-service.");
    }
}
