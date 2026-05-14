package com.stockpro.alert.dto;

import com.stockpro.alert.entity.Alert.AlertType;
import com.stockpro.alert.entity.Alert.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertRequest {

    @NotNull(message = "Recipient ID is required")
    private Long recipientId;

    @NotNull(message = "Alert type is required")
    private AlertType type;

    @Builder.Default
    private Severity severity = Severity.INFO;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Message is required")
    private String message;

    private Long   relatedProductId;
    private Long   relatedWarehouseId;

    @Builder.Default
    private String channel = "IN_APP";
}
