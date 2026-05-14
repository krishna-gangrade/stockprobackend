package com.stockpro.alert.repository;

import com.stockpro.alert.entity.Alert;
import com.stockpro.alert.entity.Alert.AlertType;
import com.stockpro.alert.entity.Alert.Severity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findByRecipientId(Long recipientId);

    List<Alert> findByRecipientIdAndIsRead(Long recipientId, boolean isRead);

    List<Alert> findByRecipientIdAndIsAcknowledged(Long recipientId, boolean isAcknowledged);

    List<Alert> findByType(AlertType type);

    List<Alert> findBySeverity(Severity severity);

    List<Alert> findByRelatedProductId(Long productId);

    List<Alert> findByRelatedWarehouseId(Long warehouseId);

    // Count unread alerts for a recipient — used for badge count in UI
    int countByRecipientIdAndIsRead(Long recipientId, boolean isRead);

    // Count unacknowledged alerts for a recipient
    int countByRecipientIdAndIsAcknowledged(Long recipientId, boolean isAcknowledged);
}
