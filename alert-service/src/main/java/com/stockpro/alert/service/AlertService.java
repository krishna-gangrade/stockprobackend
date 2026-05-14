package com.stockpro.alert.service;

import com.stockpro.alert.dto.AlertRequest;
import com.stockpro.alert.dto.AlertResponse;
import com.stockpro.alert.dto.BulkAlertRequest;
import com.stockpro.alert.entity.Alert.AlertType;
import com.stockpro.alert.entity.Alert.Severity;

import java.util.List;

public interface AlertService {

    // Send alerts
    AlertResponse        sendAlert(AlertRequest request);
    List<AlertResponse>  sendBulkAlert(BulkAlertRequest request);

    // Read
    AlertResponse        getById(Long id);
    List<AlertResponse>  getByRecipient(Long recipientId);
    List<AlertResponse>  getUnreadByRecipient(Long recipientId);
    List<AlertResponse>  getUnacknowledgedByRecipient(Long recipientId);
    List<AlertResponse>  getByType(AlertType type);
    List<AlertResponse>  getBySeverity(Severity severity);
    List<AlertResponse>  getAllAlerts();
    int                  getUnreadCount(Long recipientId);

    // State management
    AlertResponse        markAsRead(Long alertId);
    void                 markAllReadForRecipient(Long recipientId);
    AlertResponse        acknowledge(Long alertId);

    // Delete
    void                 deleteAlert(Long alertId);
}
