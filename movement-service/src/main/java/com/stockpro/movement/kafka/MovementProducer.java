package com.stockpro.movement.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MovementProducer {

    private final KafkaTemplate<String, StockUpdateEvent> kafkaTemplate;
    private static final String TOPIC = "stock-update-events";

    @SuppressWarnings("null")
    public void sendStockUpdate(StockUpdateEvent event) {
        log.info("Publishing stock update event for product {} in warehouse {}", 
                event.getProductId(), event.getWarehouseId());
        try {
            kafkaTemplate.send(TOPIC, String.valueOf(event.getProductId()), event);
        } catch (Exception e) {
            log.warn("Kafka is unavailable. Skipping event publish for product: {}", event.getProductId());
        }
    }
}
