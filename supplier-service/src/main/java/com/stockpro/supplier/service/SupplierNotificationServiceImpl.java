package com.stockpro.supplier.service;

import com.stockpro.supplier.entity.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupplierNotificationServiceImpl implements SupplierNotificationService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Override
    public void sendSuspensionNotice(Supplier supplier) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(supplier.getEmail());
            message.setSubject("StockPro supplier account suspended");
            message.setText(
                    "Hello " + supplier.getName() + ",\n\n"
                            + "Your supplier profile with StockPro has been suspended. "
                            + "New purchase orders cannot be issued to your account until it is reactivated.\n\n"
                            + "If you believe this is a mistake, please contact our procurement team."
            );
            mailSender.send(message);
        } catch (Exception ex) {
            log.error("Failed to send suspension email to supplier {}", supplier.getEmail(), ex);
        }
    }
}
