package com.stockpro.alert.dto;

import com.stockpro.alert.entity.Alert.AlertType;
import com.stockpro.alert.entity.Alert.Severity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertResponse {

    private Long          id;
    private Long          recipientId;
    private AlertType     type;
    private Severity      severity;
    private String        title;
    private String        message;
    private Long          relatedProductId;
    private Long          relatedWarehouseId;
    private String        channel;
    private boolean       isRead;
    private boolean       isAcknowledged;
    private LocalDateTime createdAt;
}
