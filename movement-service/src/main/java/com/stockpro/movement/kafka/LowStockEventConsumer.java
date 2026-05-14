package com.stockpro.movement.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka Consumer — listens to "low-stock-events" topic.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LowStockEventConsumer {

    @KafkaListener(
            topics       = "${spring.kafka.topic.low-stock}",
            groupId      = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeLowStockEvent(LowStockEvent event) {
        log.info("LowStockEvent received | product={} | warehouse={} | available={}",
                event.getProductName(), event.getWarehouseName(), event.getAvailableQuantity());

        log.info("TODO: Call alert-service via Feign to create LOW_STOCK alerts for Inventory Managers.");
        // This logic is now moved to the Alert Service or should be handled via inter-service communication.
        // Direct repository access to other services' tables is prohibited in a microservice architecture.
    }
}
