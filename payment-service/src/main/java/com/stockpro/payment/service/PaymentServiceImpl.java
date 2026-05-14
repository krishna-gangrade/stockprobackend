package com.stockpro.payment.service;

import com.stockpro.payment.config.RazorpayProperties;
import com.stockpro.payment.dto.PaymentRequest;
import com.stockpro.payment.dto.PaymentResponse;
import com.stockpro.payment.dto.RazorpayOrderResponse;
import com.stockpro.payment.dto.SupplierReturnAdjustmentRequest;
import com.stockpro.payment.entity.Payment;
import com.stockpro.payment.exception.ApiException;
import com.stockpro.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@SuppressWarnings("null")
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentRepository paymentRepository;
    private final com.razorpay.RazorpayClient razorpayClient;
    private final RazorpayProperties razorpayProperties;

    @Value("${payment.mock.enabled:true}")
    private boolean mockEnabled;

    @jakarta.annotation.PostConstruct
    public void debugConfig() {
        log.info("PAYMENT MOCK DEBUG - Mock Enabled: {}", mockEnabled);
    }

    int cleanupDuplicatePayments() {
        Map<Long, List<Payment>> activePaymentsByPurchaseOrder = paymentRepository.findAll().stream()
                .filter(payment -> payment.getStatus() != Payment.PaymentStatus.CANCELLED)
                .collect(Collectors.groupingBy(
                        Payment::getPurchaseOrderId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        int cleanedPurchaseOrders = 0;
        for (List<Payment> duplicatePayments : activePaymentsByPurchaseOrder.values()) {
            if (duplicatePayments.size() <= 1) {
                continue;
            }

            consolidateDuplicatePayments(duplicatePayments);
            cleanedPurchaseOrders++;
        }

        if (cleanedPurchaseOrders > 0) {
            log.warn("Consolidated duplicate payment rows for {} purchase order(s)", cleanedPurchaseOrders);
        }

        return cleanedPurchaseOrders;
    }

    @Override
    public RazorpayOrderResponse createRazorpayOrder(Long paymentId) throws Exception {
        ensureRazorpayConfigured();
        Payment payment = findPayment(paymentId);
        ensureOpen(payment);

        return createAndAttachRazorpayOrder(payment);
    }

    @Override
    public PaymentResponse verifyRazorpayPayment(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature, BigDecimal mockPaidAmount) throws Exception {
        Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Payment not found for order id: " + razorpayOrderId));

        BigDecimal amountPaid;
        Payment.PaymentMethod paymentMethod;
        String transactionId;

        if (mockEnabled) {
            amountPaid = (mockPaidAmount != null) ? mockPaidAmount : payment.getAmount();
            paymentMethod = Payment.PaymentMethod.RAZORPAY;
            transactionId = "MOCK_TXN_" + razorpayPaymentId;
        } else {
            org.json.JSONObject options = new org.json.JSONObject();
            options.put("razorpay_order_id", razorpayOrderId);
            options.put("razorpay_payment_id", razorpayPaymentId);
            options.put("razorpay_signature", razorpaySignature);

            boolean isValid = com.razorpay.Utils.verifyPaymentSignature(options, razorpayProperties.getSecret());
            if (!isValid) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid payment signature");
            }

            // Populate data directly from Razorpay
            com.razorpay.Payment rzpPayment = razorpayClient.payments.fetch(razorpayPaymentId);
            amountPaid = new BigDecimal(rzpPayment.get("amount").toString()).divide(new BigDecimal("100"));
            String method = rzpPayment.get("method");
            paymentMethod = mapRazorpayMethod(method);
            transactionId = razorpayPaymentId;
        }

        BigDecimal totalPaidAmount = payment.getPaidAmount().add(amountPaid);
        validateAmounts(payment.getAmount(), totalPaidAmount);

        payment.setRazorpayPaymentId(razorpayPaymentId);
        payment.setPaidAmount(totalPaidAmount);
        payment.setStatus(resolveStatus(payment.getAmount(), totalPaidAmount));
        payment.setPaymentMethod(paymentMethod);
        payment.setTransactionReference(transactionId);
        payment.setPaymentDate(LocalDate.now());

        return toResponse(paymentRepository.save(payment));
    }

    private Payment.PaymentMethod mapRazorpayMethod(String method) {
        return switch (method.toLowerCase()) {
            case "upi" -> Payment.PaymentMethod.UPI;
            case "card" -> Payment.PaymentMethod.CARD;
            case "netbanking" -> Payment.PaymentMethod.BANK_TRANSFER;
            default -> Payment.PaymentMethod.RAZORPAY;
        };
    }


    @Override
    public RazorpayOrderResponse createPayment(PaymentRequest request, Long createdById) throws Exception {
        ensureRazorpayConfigured();

        boolean hasOpenOrCompletedPayment = paymentRepository.findByPurchaseOrderId(request.getPurchaseOrderId())
                .stream()
                .anyMatch(p -> p.getStatus() != Payment.PaymentStatus.CANCELLED);

        if (hasOpenOrCompletedPayment) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "A payment record already exists for this Purchase Order. Please continue payment from the existing row.");
        }

        validateAmounts(request.getAmount(), BigDecimal.ZERO);

        Payment payment = Payment.builder()
                .purchaseOrderId(request.getPurchaseOrderId())
                .supplierId(request.getSupplierId())
                .amount(request.getAmount())
                .paidAmount(BigDecimal.ZERO)
                .status(Payment.PaymentStatus.PENDING)
                .paymentMethod(Payment.PaymentMethod.RAZORPAY)
                .dueDate(request.getDueDate())
                .notes(request.getNotes())
                .createdById(createdById)
                .build();

        return createAndAttachRazorpayOrder(paymentRepository.save(payment));
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getById(Long id) {
        return toResponse(findPayment(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getAll() {
        return paymentRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getByPurchaseOrder(Long purchaseOrderId) {
        return paymentRepository.findByPurchaseOrderId(purchaseOrderId).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getBySupplier(Long supplierId) {
        return paymentRepository.findBySupplierId(supplierId).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getByStatus(Payment.PaymentStatus status) {
        return paymentRepository.findByStatus(status).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getOverduePayments() {
        return paymentRepository.findByDueDateBeforeAndStatusIn(
                LocalDate.now(),
                List.of(Payment.PaymentStatus.PENDING, Payment.PaymentStatus.PARTIAL)
        ).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getByDateRange(java.time.LocalDate from, java.time.LocalDate to) {
        java.time.LocalDateTime start = from.atStartOfDay();
        java.time.LocalDateTime end = to.atTime(23, 59, 59);
        return paymentRepository.findByCreatedAtBetween(start, end).stream()
                .map(this::toResponse).toList();
    }

    @Override
    public PaymentResponse markFailed(Long id, String reason) {
        Payment payment = findPayment(id);
        ensureOpen(payment);
        payment.setStatus(Payment.PaymentStatus.FAILED);
        payment.setNotes(appendNote(payment.getNotes(), "Failed: " + reason));
        return toResponse(paymentRepository.save(payment));
    }

    @Override
    public PaymentResponse applySupplierReturn(Long purchaseOrderId, SupplierReturnAdjustmentRequest request) {
        Payment payment = findActivePaymentByPurchaseOrder(purchaseOrderId);
        BigDecimal returnAmount = normalizePositiveAmount(request.getAmount(), "Supplier return amount must be greater than zero");

        return switch (request.getAction()) {
            case REDUCE_PAYABLE -> applyReducePayable(payment, returnAmount, request.getReason());
            case RECORD_SUPPLIER_REFUND -> applySupplierRefund(payment, returnAmount, request.getReason());
        };
    }

    @Override
    public void cancelPayment(Long id, String reason) {
        Payment payment = findPayment(id);
        if (payment.getStatus() == Payment.PaymentStatus.PAID) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Paid payments cannot be cancelled");
        }
        payment.setStatus(Payment.PaymentStatus.CANCELLED);
        payment.setNotes(appendNote(payment.getNotes(), "Cancelled: " + reason));
        paymentRepository.save(payment);
    }

    private Payment findPayment(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Payment not found with id: " + id));
    }

    private Payment findActivePaymentByPurchaseOrder(Long purchaseOrderId) {
        return paymentRepository.findByPurchaseOrderId(purchaseOrderId).stream()
                .filter(payment -> payment.getStatus() != Payment.PaymentStatus.CANCELLED)
                .sorted(Comparator
                        .comparing(Payment::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo))
                        .thenComparing(Payment::getId, Comparator.nullsLast(Long::compareTo)))
                .findFirst()
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "No active payment found for purchase order: " + purchaseOrderId
                ));
    }

    private void consolidateDuplicatePayments(List<Payment> duplicatePayments) {
        List<Payment> orderedPayments = duplicatePayments.stream()
                .sorted(Comparator
                        .comparing(Payment::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo))
                        .thenComparing(Payment::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();

        Payment canonicalPayment = orderedPayments.get(0);
        List<Payment> duplicates = orderedPayments.subList(1, orderedPayments.size());
        Payment referencePayment = orderedPayments.stream()
                .max(Comparator
                        .comparing(Payment::getPaidAmount, Comparator.nullsLast(BigDecimal::compareTo))
                        .thenComparing(Payment::getUpdatedAt, Comparator.nullsLast(LocalDateTime::compareTo))
                        .thenComparing(Payment::getId, Comparator.nullsLast(Long::compareTo)))
                .orElse(canonicalPayment);

        BigDecimal canonicalAmount = orderedPayments.stream()
                .map(Payment::getAmount)
                .filter(Objects::nonNull)
                .max(BigDecimal::compareTo)
                .orElse(canonicalPayment.getAmount());

        BigDecimal totalPaid = orderedPayments.stream()
                .map(Payment::getPaidAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal normalizedPaid = totalPaid.min(canonicalAmount);

        canonicalPayment.setAmount(canonicalAmount);
        canonicalPayment.setPaidAmount(normalizedPaid);
        canonicalPayment.setStatus(resolveStatus(canonicalAmount, normalizedPaid));
        canonicalPayment.setPaymentMethod(referencePayment.getPaymentMethod());
        canonicalPayment.setTransactionReference(firstNonBlank(
                referencePayment.getTransactionReference(),
                canonicalPayment.getTransactionReference()
        ));
        canonicalPayment.setRazorpayOrderId(firstNonBlank(
                referencePayment.getRazorpayOrderId(),
                canonicalPayment.getRazorpayOrderId()
        ));
        canonicalPayment.setRazorpayPaymentId(firstNonBlank(
                referencePayment.getRazorpayPaymentId(),
                canonicalPayment.getRazorpayPaymentId()
        ));
        canonicalPayment.setPaymentDate(orderedPayments.stream()
                .map(Payment::getPaymentDate)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(canonicalPayment.getPaymentDate()));
        if (canonicalPayment.getDueDate() == null) {
            canonicalPayment.setDueDate(orderedPayments.stream()
                    .map(Payment::getDueDate)
                    .filter(Objects::nonNull)
                    .min(LocalDate::compareTo)
                    .orElse(null));
        }
        canonicalPayment.setNotes(buildCleanupNotes(canonicalPayment, duplicates, totalPaid, canonicalAmount));

        for (Payment duplicate : duplicates) {
            duplicate.setStatus(Payment.PaymentStatus.CANCELLED);
            duplicate.setNotes(appendNote(
                    duplicate.getNotes(),
                    "Merged into payment #" + canonicalPayment.getId() + " during duplicate cleanup."
            ));
        }

        paymentRepository.saveAll(orderedPayments);
    }

    private RazorpayOrderResponse createAndAttachRazorpayOrder(Payment payment) throws Exception {
        BigDecimal balanceAmount = payment.getAmount().subtract(payment.getPaidAmount());
        if (balanceAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This Purchase Order has no remaining balance.");
        }

        int amountInPaise = toPaise(balanceAmount);

        String orderId;
        if (mockEnabled) {
            log.info("Using MOCK payment order for payment ID: {}", payment.getId());
            orderId = "order_mock_" + System.currentTimeMillis();
        } else {
            log.info("Attempting to create REAL Razorpay order for payment ID: {}", payment.getId());
            org.json.JSONObject orderRequest = new org.json.JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "payment_" + payment.getId());
            orderRequest.put("payment_capture", 1);

            com.razorpay.Order order = razorpayClient.orders.create(orderRequest);
            orderId = order.get("id");
        }

        payment.setRazorpayOrderId(orderId);
        payment.setPaymentMethod(Payment.PaymentMethod.RAZORPAY);
        Payment savedPayment = paymentRepository.save(payment);

        return RazorpayOrderResponse.builder()
                .payment(toResponse(savedPayment))
                .razorpayKeyId(razorpayProperties.getId())
                .razorpayOrderId(orderId)
                .amount(amountInPaise)
                .currency("INR")
                .build();
    }

    private void ensureRazorpayConfigured() {
        if (mockEnabled) return;

        String razorpayKeyId = razorpayProperties.getId();
        String razorpayKeySecret = razorpayProperties.getSecret();
        if (razorpayKeyId == null || razorpayKeySecret == null
                || razorpayKeyId.isBlank() || razorpayKeySecret.isBlank()
                || razorpayKeyId.contains("placeholder") || razorpayKeySecret.contains("placeholder")) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Razorpay is not configured. Set RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET.");
        }
    }

    private int toPaise(BigDecimal amount) {
        return amount.multiply(new BigDecimal("100"))
                .setScale(0, RoundingMode.HALF_UP)
                .intValueExact();
    }

    private void validateAmounts(BigDecimal amount, BigDecimal paidAmount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Payment amount must be greater than zero");
        }
        if (paidAmount == null || paidAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Paid amount cannot be negative");
        }
        if (paidAmount.compareTo(amount) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Paid amount cannot exceed payment amount");
        }
    }

    private void ensureOpen(Payment payment) {
        if (payment.getStatus() == Payment.PaymentStatus.CANCELLED || payment.getStatus() == Payment.PaymentStatus.PAID) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Payment is already " + payment.getStatus());
        }
    }

    private Payment.PaymentStatus resolveStatus(BigDecimal amount, BigDecimal paidAmount) {
        if (paidAmount.compareTo(BigDecimal.ZERO) == 0) {
            return Payment.PaymentStatus.PENDING;
        }
        if (paidAmount.compareTo(amount) < 0) {
            return Payment.PaymentStatus.PARTIAL;
        }
        return Payment.PaymentStatus.PAID;
    }

    private String appendNote(String existing, String note) {
        if (existing == null || existing.isBlank()) {
            return note;
        }
        return existing + "\n" + note;
    }

    private BigDecimal normalizePositiveAmount(BigDecimal amount, String message) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, message);
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private PaymentResponse applyReducePayable(Payment payment, BigDecimal returnAmount, String reason) {
        BigDecimal openBalance = payment.getAmount().subtract(payment.getPaidAmount());
        if (openBalance.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "This payment has no unpaid balance to reduce. Record a supplier refund instead.");
        }
        if (returnAmount.compareTo(openBalance) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Supplier return exceeds the unpaid balance for this purchase order.");
        }

        BigDecimal nextAmount = payment.getAmount().subtract(returnAmount);
        payment.setAmount(nextAmount);
        payment.setStatus(resolveAdjustedStatus(nextAmount, payment.getPaidAmount()));
        payment.setNotes(appendNote(
                payment.getNotes(),
                "Supplier return reduced payable by " + returnAmount.toPlainString() + "."
                        + formatReason(reason)
        ));
        return toResponse(paymentRepository.save(payment));
    }

    private PaymentResponse applySupplierRefund(Payment payment, BigDecimal returnAmount, String reason) {
        if (returnAmount.compareTo(payment.getPaidAmount()) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Supplier refund cannot exceed the amount already paid.");
        }

        BigDecimal nextAmount = payment.getAmount().subtract(returnAmount);
        BigDecimal nextPaidAmount = payment.getPaidAmount().subtract(returnAmount);
        if (nextAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Supplier refund would reduce the payable amount below zero.");
        }

        payment.setAmount(nextAmount);
        payment.setPaidAmount(nextPaidAmount);
        payment.setStatus(resolveAdjustedStatus(nextAmount, nextPaidAmount));
        payment.setNotes(appendNote(
                payment.getNotes(),
                "Supplier refund recorded for " + returnAmount.toPlainString() + "."
                        + formatReason(reason)
        ));
        return toResponse(paymentRepository.save(payment));
    }

    private Payment.PaymentStatus resolveAdjustedStatus(BigDecimal amount, BigDecimal paidAmount) {
        if (amount.compareTo(BigDecimal.ZERO) == 0 && paidAmount.compareTo(BigDecimal.ZERO) == 0) {
            return Payment.PaymentStatus.CANCELLED;
        }
        return resolveStatus(amount, paidAmount);
    }

    private String formatReason(String reason) {
        return (reason == null || reason.isBlank()) ? "" : " Reason: " + reason.trim();
    }

    private String buildCleanupNotes(Payment canonicalPayment, List<Payment> duplicates, BigDecimal totalPaid, BigDecimal canonicalAmount) {
        String duplicateIds = duplicates.stream()
                .map(Payment::getId)
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .collect(Collectors.joining(", "));

        String cleanupNote = "Duplicate cleanup merged payment row(s)"
                + (duplicateIds.isBlank() ? "" : " #" + duplicateIds)
                + " for PO #" + canonicalPayment.getPurchaseOrderId() + ".";

        if (totalPaid.compareTo(canonicalAmount) > 0) {
            cleanupNote += " Paid amount was capped at PO amount " + canonicalAmount.toPlainString()
                    + " from duplicate total " + totalPaid.toPlainString() + ".";
        }

        return appendNote(canonicalPayment.getNotes(), cleanupNote);
    }

    private String firstNonBlank(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred;
        }
        return fallback;
    }

    private PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .purchaseOrderId(payment.getPurchaseOrderId())
                .supplierId(payment.getSupplierId())
                .amount(payment.getAmount())
                .paidAmount(payment.getPaidAmount())
                .balanceAmount(payment.getAmount().subtract(payment.getPaidAmount()))
                .status(payment.getStatus())
                .paymentMethod(payment.getPaymentMethod())
                .transactionReference(payment.getTransactionReference())
                .razorpayOrderId(payment.getRazorpayOrderId())
                .razorpayPaymentId(payment.getRazorpayPaymentId())
                .paymentDate(payment.getPaymentDate())
                .dueDate(payment.getDueDate())
                .notes(payment.getNotes())
                .createdById(payment.getCreatedById())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
