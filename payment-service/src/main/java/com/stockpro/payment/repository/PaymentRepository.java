package com.stockpro.payment.repository;

import com.stockpro.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByPurchaseOrderId(Long purchaseOrderId);
    List<Payment> findBySupplierId(Long supplierId);
    List<Payment> findByStatus(Payment.PaymentStatus status);
    List<Payment> findByDueDateBeforeAndStatusIn(LocalDate date, List<Payment.PaymentStatus> statuses);
    java.util.Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);
    java.util.Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);
    List<Payment> findByCreatedAtBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);
}
