package com.stockpro.purchase.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka Producer — publishes Purchase Order related events.
 *
 * KafkaTemplate is Spring's wrapper around the Kafka producer API.
 * send() is async — it returns a CompletableFuture so we log success/failure
 * without blocking the HTTP response.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class POApprovalProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topic.po-approval:po-approval-events}")
    private String poApprovalTopic;

    @Value("${kafka.topic.po-created:po-created-events}")
    private String poCreatedTopic;

    public void publishApprovalEvent(POApprovalEvent event) {
        publishEvent(poApprovalTopic, String.valueOf(event.getPoId()), event, "POApprovalEvent");
    }

    public void publishCreatedEvent(POCreatedEvent event) {
        publishEvent(poCreatedTopic, String.valueOf(event.getPoId()), event, "POCreatedEvent");
    }

    private void publishEvent(String topic, String key, Object event, String type) {
        try {
            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send(topic, key, event);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("{} published | key={} | offset={}",
                            type, key, result.getRecordMetadata().offset());
                } else {
                    log.warn("Failed to publish {} | key={} | error={}",
                            type, key, ex.getMessage());
                }
            });
        } catch (Exception ex) {
            log.warn("Kafka is unavailable. Skipping {} | key={} | error={}",
                    type, key, ex.getMessage());
        }
    }
}
