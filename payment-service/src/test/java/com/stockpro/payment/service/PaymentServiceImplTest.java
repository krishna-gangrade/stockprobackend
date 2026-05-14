package com.stockpro.payment.service;

import com.stockpro.payment.config.RazorpayProperties;
import com.stockpro.payment.dto.PaymentRequest;
import com.stockpro.payment.dto.SupplierReturnAdjustmentRequest;
import com.stockpro.payment.entity.Payment;
import com.stockpro.payment.exception.ApiException;
import com.stockpro.payment.repository.PaymentRepository;
import com.razorpay.RazorpayClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings("null")
class PaymentServiceImplTest {

    private PaymentRepository paymentRepository;
    private PaymentServiceImpl paymentService;
    private RazorpayProperties razorpayProperties;

    @BeforeEach
    void setUp() {
        paymentRepository = mock(PaymentRepository.class);
        razorpayProperties = new RazorpayProperties();
        razorpayProperties.setId("rzp_test_123");
        razorpayProperties.setSecret("secret_123");
        paymentService = new PaymentServiceImpl(paymentRepository, mock(RazorpayClient.class), razorpayProperties);
    }

    @Test
    void createPaymentRejectsNonPositiveAmountBeforeGatewayCall() {
        PaymentRequest request = new PaymentRequest();
        request.setPurchaseOrderId(10L);
        request.setSupplierId(20L);
        request.setAmount(BigDecimal.ZERO);

        assertThatThrownBy(() -> paymentService.createPayment(request, 1L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("greater than zero");

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void getByIdReturnsPaymentResponse() {
        Payment payment = Payment.builder()
                .id(1L)
                .purchaseOrderId(10L)
                .supplierId(20L)
                .amount(new BigDecimal("1000.00"))
                .paidAmount(BigDecimal.ZERO)
                .status(Payment.PaymentStatus.PENDING)
                .paymentMethod(Payment.PaymentMethod.RAZORPAY)
                .createdById(5L)
                .build();

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        var response = paymentService.getById(1L);

        assertThat(response.getPaymentMethod()).isEqualTo(Payment.PaymentMethod.RAZORPAY);
        assertThat(response.getStatus()).isEqualTo(Payment.PaymentStatus.PENDING);
        assertThat(response.getBalanceAmount()).isEqualByComparingTo("1000.00");
    }

    @Test
    void cancelPaymentRejectsPaidPayment() {
        Payment payment = Payment.builder()
                .id(1L)
                .amount(new BigDecimal("100.00"))
                .paidAmount(new BigDecimal("100.00"))
                .status(Payment.PaymentStatus.PAID)
                .paymentMethod(Payment.PaymentMethod.CASH)
                .createdById(5L)
                .build();

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.cancelPayment(1L, "Wrong supplier"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Paid payments cannot be cancelled");

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void markFailedAppendsReasonAndSaves() {
        Payment payment = Payment.builder()
                .id(1L)
                .purchaseOrderId(10L)
                .supplierId(20L)
                .amount(new BigDecimal("100.00"))
                .paidAmount(BigDecimal.ZERO)
                .status(Payment.PaymentStatus.PENDING)
                .paymentMethod(Payment.PaymentMethod.CARD)
                .createdById(5L)
                .notes("Gateway timeout")
                .build();

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        paymentService.markFailed(1L, "Bank declined");

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(Payment.PaymentStatus.FAILED);
        assertThat(captor.getValue().getNotes()).contains("Gateway timeout", "Failed: Bank declined");
    }

    @Test
    void createPaymentFailsFastWhenRazorpayKeysAreMissing() {
        razorpayProperties.setId("rzp_test_placeholder");
        razorpayProperties.setSecret("rzp_test_secret_placeholder");

        PaymentRequest request = new PaymentRequest();
        request.setPurchaseOrderId(10L);
        request.setSupplierId(20L);
        request.setAmount(new BigDecimal("1000.00"));

        assertThatThrownBy(() -> paymentService.createPayment(request, 1L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Razorpay is not configured");
    }

    @Test
    void cleanupDuplicatePaymentsMergesActiveRowsIntoCanonicalPayment() {
        Payment canonical = Payment.builder()
                .id(10L)
                .purchaseOrderId(99L)
                .supplierId(20L)
                .amount(new BigDecimal("280000.00"))
                .paidAmount(BigDecimal.ZERO)
                .status(Payment.PaymentStatus.PENDING)
                .paymentMethod(Payment.PaymentMethod.RAZORPAY)
                .dueDate(LocalDate.of(2026, 4, 30))
                .createdById(5L)
                .createdAt(LocalDateTime.of(2026, 4, 29, 10, 0))
                .build();

        Payment duplicate = Payment.builder()
                .id(11L)
                .purchaseOrderId(99L)
                .supplierId(20L)
                .amount(new BigDecimal("280000.00"))
                .paidAmount(new BigDecimal("140000.00"))
                .status(Payment.PaymentStatus.PARTIAL)
                .paymentMethod(Payment.PaymentMethod.UPI)
                .transactionReference("txn_123")
                .razorpayOrderId("order_123")
                .razorpayPaymentId("payment_123")
                .paymentDate(LocalDate.of(2026, 4, 30))
                .createdById(5L)
                .createdAt(LocalDateTime.of(2026, 4, 29, 11, 0))
                .updatedAt(LocalDateTime.of(2026, 4, 30, 9, 0))
                .build();

        when(paymentRepository.findAll()).thenReturn(List.of(canonical, duplicate));
        when(paymentRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        int cleaned = paymentService.cleanupDuplicatePayments();

        assertThat(cleaned).isEqualTo(1);
        verify(paymentRepository).saveAll(anyList());
        assertThat(canonical.getPaidAmount()).isEqualByComparingTo("140000.00");
        assertThat(canonical.getStatus()).isEqualTo(Payment.PaymentStatus.PARTIAL);
        assertThat(canonical.getPaymentMethod()).isEqualTo(Payment.PaymentMethod.UPI);
        assertThat(canonical.getTransactionReference()).isEqualTo("txn_123");
        assertThat(canonical.getRazorpayPaymentId()).isEqualTo("payment_123");
        assertThat(canonical.getNotes()).contains("Duplicate cleanup merged payment row(s) #11 for PO #99.");
        assertThat(duplicate.getStatus()).isEqualTo(Payment.PaymentStatus.CANCELLED);
        assertThat(duplicate.getNotes()).contains("Merged into payment #10 during duplicate cleanup.");
    }

    @Test
    void applySupplierReturnCanReduceOpenPayable() {
        Payment payment = Payment.builder()
                .id(5L)
                .purchaseOrderId(42L)
                .supplierId(20L)
                .amount(new BigDecimal("1000.00"))
                .paidAmount(new BigDecimal("200.00"))
                .status(Payment.PaymentStatus.PARTIAL)
                .paymentMethod(Payment.PaymentMethod.RAZORPAY)
                .createdById(5L)
                .createdAt(LocalDateTime.of(2026, 5, 1, 10, 0))
                .build();

        when(paymentRepository.findByPurchaseOrderId(42L)).thenReturn(List.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = paymentService.applySupplierReturn(42L, SupplierReturnAdjustmentRequest.builder()
                .amount(new BigDecimal("150.00"))
                .action(SupplierReturnAdjustmentRequest.AdjustmentAction.REDUCE_PAYABLE)
                .reason("Defective units returned")
                .build());

        assertThat(response.getAmount()).isEqualByComparingTo("850.00");
        assertThat(response.getPaidAmount()).isEqualByComparingTo("200.00");
        assertThat(response.getStatus()).isEqualTo(Payment.PaymentStatus.PARTIAL);
        assertThat(response.getNotes()).contains("Supplier return reduced payable by 150.00");
    }

    @Test
    void applySupplierReturnCanRecordSupplierRefund() {
        Payment payment = Payment.builder()
                .id(6L)
                .purchaseOrderId(43L)
                .supplierId(20L)
                .amount(new BigDecimal("1000.00"))
                .paidAmount(new BigDecimal("1000.00"))
                .status(Payment.PaymentStatus.PAID)
                .paymentMethod(Payment.PaymentMethod.RAZORPAY)
                .createdById(5L)
                .createdAt(LocalDateTime.of(2026, 5, 1, 10, 0))
                .build();

        when(paymentRepository.findByPurchaseOrderId(43L)).thenReturn(List.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = paymentService.applySupplierReturn(43L, SupplierReturnAdjustmentRequest.builder()
                .amount(new BigDecimal("250.00"))
                .action(SupplierReturnAdjustmentRequest.AdjustmentAction.RECORD_SUPPLIER_REFUND)
                .reason("Returned excess stock")
                .build());

        assertThat(response.getAmount()).isEqualByComparingTo("750.00");
        assertThat(response.getPaidAmount()).isEqualByComparingTo("750.00");
        assertThat(response.getStatus()).isEqualTo(Payment.PaymentStatus.PAID);
        assertThat(response.getNotes()).contains("Supplier refund recorded for 250.00");
    }
}
