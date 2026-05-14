package com.stockpro.alert.service;

import com.stockpro.alert.dto.AlertRequest;
import com.stockpro.alert.dto.AlertResponse;
import com.stockpro.alert.dto.BulkAlertRequest;
import com.stockpro.alert.entity.Alert;
import com.stockpro.alert.entity.Alert.AlertType;
import com.stockpro.alert.entity.Alert.Severity;
import com.stockpro.alert.repository.AlertRepository;
import com.stockpro.alert.exception.ApiException;
import com.stockpro.alert.client.AuthClient;
import com.stockpro.alert.dto.external.UserResponse;
import com.stockpro.alert.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertServiceImpl implements AlertService {

    private final AlertRepository alertRepository;
    private final AuthClient authClient;
    private final JavaMailSender mailSender;

    // =====================================================================
    // SEND ALERTS
    // =====================================================================

    @Override
    @Transactional
    @SuppressWarnings("null")
    public AlertResponse sendAlert(AlertRequest request) {
        Alert alert = Alert.builder()
                .recipientId(request.getRecipientId())
                .type(request.getType())
                .severity(request.getSeverity())
                .title(request.getTitle())
                .message(request.getMessage())
                .relatedProductId(request.getRelatedProductId())
                .relatedWarehouseId(request.getRelatedWarehouseId())
                .channel(request.getChannel() != null ? request.getChannel() : "IN_APP")
                .isRead(false)
                .isAcknowledged(false)
                .createdAt(LocalDateTime.now())
                .build();

        Alert saved = alertRepository.save(alert);

        // CRITICAL alerts also send an email
        if (request.getSeverity() == Severity.CRITICAL) {
            sendEmailAlert(saved);
        }

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public List<AlertResponse> sendBulkAlert(BulkAlertRequest request) {
        List<AlertResponse> responses = new ArrayList<>();

        for (Long recipientId : request.getRecipientIds()) {
            AlertRequest alertRequest = AlertRequest.builder()
                    .recipientId(recipientId)
                    .type(request.getType())
                    .severity(request.getSeverity())
                    .title(request.getTitle())
                    .message(request.getMessage())
                    .relatedProductId(request.getRelatedProductId())
                    .relatedWarehouseId(request.getRelatedWarehouseId())
                    .build();

            responses.add(sendAlert(alertRequest));
        }

        log.info("Bulk alert sent to {} recipients | type={}", 
                request.getRecipientIds().size(), request.getType());
        return responses;
    }

    // =====================================================================
    // READ
    // =====================================================================

    @Override
    @SuppressWarnings("null")
    public AlertResponse getById(Long id) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ApiException(
                        "Alert not found with id: " + id, HttpStatus.NOT_FOUND));
        return mapToResponse(alert);
    }

    @Override
    public List<AlertResponse> getAllAlerts() {
        return alertRepository.findAll()
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<AlertResponse> getByRecipient(Long recipientId) {
        return alertRepository.findByRecipientId(recipientId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<AlertResponse> getUnreadByRecipient(Long recipientId) {
        return alertRepository.findByRecipientIdAndIsRead(recipientId, false)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<AlertResponse> getUnacknowledgedByRecipient(Long recipientId) {
        return alertRepository.findByRecipientIdAndIsAcknowledged(recipientId, false)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<AlertResponse> getByType(AlertType type) {
        return alertRepository.findByType(type)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<AlertResponse> getBySeverity(Severity severity) {
        return alertRepository.findBySeverity(severity)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public int getUnreadCount(Long recipientId) {
        return alertRepository.countByRecipientIdAndIsRead(recipientId, false);
    }

    // =====================================================================
    // STATE MANAGEMENT
    // =====================================================================

    @Override
    @Transactional
    public AlertResponse markAsRead(Long alertId) {
        Alert alert = findAlertOrThrow(alertId);
        alert.setRead(true);
        return mapToResponse(alertRepository.save(alert));
    }

    @Override
    @Transactional
    public void markAllReadForRecipient(Long recipientId) {
        List<Alert> unread = alertRepository.findByRecipientIdAndIsRead(recipientId, false);
        unread.forEach(a -> a.setRead(true));
        alertRepository.saveAll(unread);
        log.info("Marked {} alerts as read for recipientId={}", unread.size(), recipientId);
    }

    @Override
    @Transactional
    public AlertResponse acknowledge(Long alertId) {
        Alert alert = findAlertOrThrow(alertId);
        alert.setAcknowledged(true);
        alert.setRead(true); // acknowledging also marks as read
        return mapToResponse(alertRepository.save(alert));
    }

    // =====================================================================
    // DELETE
    // =====================================================================

    @Override
    @Transactional
    @SuppressWarnings("null")
    public void deleteAlert(Long alertId) {
        if (!alertRepository.existsById(alertId)) {
            throw new ApiException("Alert not found with id: " + alertId, HttpStatus.NOT_FOUND);
        }
        alertRepository.deleteById(alertId);
    }

    // =====================================================================
    // EMAIL DISPATCH — only for CRITICAL severity alerts
    // =====================================================================

    private void sendEmailAlert(Alert alert) {
        log.info("Email alert triggered for recipientId={} | severity=CRITICAL", alert.getRecipientId());
        try {
            ApiResponse<UserResponse> response = authClient.getUserById(alert.getRecipientId());
            if (response != null && response.isSuccess() && response.getData() != null) {
                String email = response.getData().getEmail();
                log.info("Sending email to: {}", email);
                
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(email);
                message.setSubject("CRITICAL ALERT: " + alert.getTitle());
                message.setText(alert.getMessage());
                mailSender.send(message);
                
                log.info("Email sent successfully to {}", email);
            } else {
                log.warn("Could not fetch user email for recipientId={}", alert.getRecipientId());
            }
        } catch (Exception e) {
            log.error("Failed to send email alert: {}", e.getMessage());
        }
    }

    // =====================================================================
    // PRIVATE HELPERS
    // =====================================================================

    @SuppressWarnings("null")
    private Alert findAlertOrThrow(Long id) {
        return alertRepository.findById(id)
                .orElseThrow(() -> new ApiException(
                        "Alert not found with id: " + id, HttpStatus.NOT_FOUND));
    }

    private AlertResponse mapToResponse(Alert a) {
        return AlertResponse.builder()
                .id(a.getId())
                .recipientId(a.getRecipientId())
                .type(a.getType())
                .severity(a.getSeverity())
                .title(a.getTitle())
                .message(a.getMessage())
                .relatedProductId(a.getRelatedProductId())
                .relatedWarehouseId(a.getRelatedWarehouseId())
                .channel(a.getChannel())
                .isRead(a.isRead())
                .isAcknowledged(a.isAcknowledged())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
