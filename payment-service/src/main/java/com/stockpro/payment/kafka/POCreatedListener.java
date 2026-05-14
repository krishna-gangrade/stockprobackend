package com.stockpro.payment.kafka;

import com.stockpro.payment.dto.POCreatedEvent;
import com.stockpro.payment.entity.Payment;
import com.stockpro.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "kafka", name = "enabled", havingValue = "true")
@SuppressWarnings("null")
public class POCreatedListener {

    private final PaymentRepository paymentRepository;

    @KafkaListener(topics = "${kafka.topic.po-created:po-created-events}", groupId = "payment-group")
    public void handlePOCreated(POCreatedEvent event) {
        log.info("Received POCreatedEvent for PO ID: {}", event.getPoId());

        // Check if payment already exists to avoid duplicates
        if (!paymentRepository.findByPurchaseOrderId(event.getPoId()).isEmpty()) {
            log.warn("Payment already exists for PO ID: {}. Skipping.", event.getPoId());
            return;
        }

        Payment payment = Payment.builder()
                .purchaseOrderId(event.getPoId())
                .supplierId(event.getSupplierId())
                .amount(event.getTotalAmount())
                .paidAmount(BigDecimal.ZERO)
                .status(Payment.PaymentStatus.PENDING)
                .paymentMethod(Payment.PaymentMethod.RAZORPAY)
                .dueDate(event.getDueDate())
                .notes("Automatically created from Purchase Order #" + event.getPoId())
                .createdById(event.getCreatedById())
                .createdAt(event.getCreatedAt() != null ? event.getCreatedAt() : java.time.LocalDateTime.now())
                .build();

        paymentRepository.save(payment);
        log.info("Automatically created PENDING payment for PO ID: {}", event.getPoId());
    }
}
