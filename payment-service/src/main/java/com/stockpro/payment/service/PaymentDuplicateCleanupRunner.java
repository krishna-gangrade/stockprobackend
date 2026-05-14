package com.stockpro.payment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentDuplicateCleanupRunner {

    private final PaymentServiceImpl paymentService;

    @Value("${payment.cleanup.duplicates.enabled:true}")
    private boolean duplicateCleanupEnabled;

    @EventListener(ApplicationReadyEvent.class)
    public void cleanupDuplicatesOnStartup() {
        if (!duplicateCleanupEnabled) {
            return;
        }

        paymentService.cleanupDuplicatePayments();
    }
}
