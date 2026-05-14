package com.stockpro.alert;

import com.stockpro.alert.dto.AlertRequest;
import com.stockpro.alert.dto.AlertResponse;
import com.stockpro.alert.dto.BulkAlertRequest;
import com.stockpro.alert.entity.Alert;
import com.stockpro.alert.entity.Alert.AlertType;
import com.stockpro.alert.entity.Alert.Severity;
import com.stockpro.alert.repository.AlertRepository;
import com.stockpro.alert.service.AlertServiceImpl;
import com.stockpro.alert.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
public class AlertServiceTest {

    @Mock private AlertRepository alertRepository;
    @Mock private JavaMailSender  mailSender;

    @InjectMocks
    private AlertServiceImpl alertService;

    private Alert sampleAlert;

    @BeforeEach
    void setUp() {
        sampleAlert = Alert.builder()
                .id(1L).recipientId(1L)
                .type(AlertType.LOW_STOCK)
                .severity(Severity.WARNING)
                .title("Low Stock Alert")
                .message("Product X is below reorder level")
                .isRead(false).isAcknowledged(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ===== sendAlert() =====

    @Test
    @DisplayName("sendAlert - INFO alert saved without sending email")
    void sendAlert_infoSeverity_noEmail() {
        AlertRequest request = AlertRequest.builder()
                .recipientId(1L)
                .type(AlertType.LOW_STOCK)
                .severity(Severity.INFO)
                .title("Low Stock Alert")
                .message("Product X below reorder level")
                .build();

        when(alertRepository.save(any(Alert.class))).thenReturn(sampleAlert);

        AlertResponse response = alertService.sendAlert(request);

        assertThat(response.getType()).isEqualTo(AlertType.LOW_STOCK);
        assertThat(response.isRead()).isFalse();
        verify(alertRepository).save(any(Alert.class));
    }

    @Test
    @DisplayName("sendAlert - CRITICAL alert saved")
    void sendAlert_criticalSeverity_saved() {
        AlertRequest request = AlertRequest.builder()
                .recipientId(1L)
                .type(AlertType.OVERDUE_RECEIPT)
                .severity(Severity.CRITICAL)
                .title("Overdue PO Alert")
                .message("PO-001 is 3 days overdue")
                .build();

        Alert criticalAlert = Alert.builder()
                .id(2L).recipientId(1L)
                .type(AlertType.OVERDUE_RECEIPT)
                .severity(Severity.CRITICAL)
                .title("Overdue PO Alert")
                .message("PO-001 is 3 days overdue")
                .isRead(false).isAcknowledged(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(alertRepository.save(any(Alert.class))).thenReturn(criticalAlert);

        AlertResponse response = alertService.sendAlert(request);

        assertThat(response.getSeverity()).isEqualTo(Severity.CRITICAL);
        verify(alertRepository).save(any(Alert.class));
    }

    // ===== sendBulkAlert() =====

    @Test
    @DisplayName("sendBulkAlert - creates one alert per recipient")
    void sendBulkAlert_createsAlertPerRecipient() {
        BulkAlertRequest request = BulkAlertRequest.builder()
                .recipientIds(List.of(1L, 2L, 3L))
                .type(AlertType.SYSTEM)
                .severity(Severity.INFO)
                .title("Platform maintenance")
                .message("System will be down for maintenance at midnight")
                .build();

        when(alertRepository.save(any(Alert.class))).thenReturn(sampleAlert);

        List<AlertResponse> responses = alertService.sendBulkAlert(request);

        // One alert saved per recipient
        assertThat(responses).hasSize(3);
        verify(alertRepository, times(3)).save(any(Alert.class));
    }

    // ===== markAsRead() =====

    @Test
    @DisplayName("markAsRead - sets isRead to true")
    void markAsRead_setsIsReadTrue() {
        when(alertRepository.findById(1L)).thenReturn(Optional.of(sampleAlert));
        when(alertRepository.save(any())).thenReturn(sampleAlert);

        alertService.markAsRead(1L);

        assertThat(sampleAlert.isRead()).isTrue();
        verify(alertRepository).save(sampleAlert);
    }

    @Test
    @DisplayName("markAsRead - throws ApiException when alert not found")
    void markAsRead_notFound_throwsException() {
        when(alertRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> alertService.markAsRead(99L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Alert not found");
    }

    // ===== acknowledge() =====

    @Test
    @DisplayName("acknowledge - sets both isAcknowledged and isRead to true")
    void acknowledge_setsBothFlags() {
        when(alertRepository.findById(1L)).thenReturn(Optional.of(sampleAlert));
        when(alertRepository.save(any())).thenReturn(sampleAlert);

        alertService.acknowledge(1L);

        assertThat(sampleAlert.isAcknowledged()).isTrue();
        assertThat(sampleAlert.isRead()).isTrue();
        verify(alertRepository).save(sampleAlert);
    }

    // ===== markAllReadForRecipient() =====

    @Test
    @DisplayName("markAllReadForRecipient - marks all unread alerts for user as read")
    void markAllReadForRecipient_marksAll() {
        Alert alert2 = Alert.builder().id(2L).recipientId(1L)
                .type(AlertType.OVERSTOCK).severity(Severity.INFO)
                .title("Overstock").message("Too much stock")
                .isRead(false).isAcknowledged(false)
                .createdAt(LocalDateTime.now()).build();

        when(alertRepository.findByRecipientIdAndIsRead(1L, false))
                .thenReturn(List.of(sampleAlert, alert2));

        alertService.markAllReadForRecipient(1L);

        assertThat(sampleAlert.isRead()).isTrue();
        assertThat(alert2.isRead()).isTrue();
        verify(alertRepository).saveAll(anyList());
    }

    // ===== getUnreadCount() =====

    @Test
    @DisplayName("getUnreadCount - returns correct unread count")
    void getUnreadCount_returnsCount() {
        when(alertRepository.countByRecipientIdAndIsRead(1L, false)).thenReturn(5);

        int count = alertService.getUnreadCount(1L);

        assertThat(count).isEqualTo(5);
    }

    // ===== deleteAlert() =====

    @Test
    @DisplayName("deleteAlert - calls deleteById when alert exists")
    void deleteAlert_success() {
        when(alertRepository.existsById(1L)).thenReturn(true);

        alertService.deleteAlert(1L);

        verify(alertRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteAlert - throws ApiException when alert not found")
    void deleteAlert_notFound_throwsException() {
        when(alertRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> alertService.deleteAlert(99L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Alert not found");

        verify(alertRepository, never()).deleteById(any());
    }
}
