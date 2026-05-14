package com.stockpro.alert.dto;

import com.stockpro.alert.entity.Alert.AlertType;
import com.stockpro.alert.entity.Alert.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkAlertRequest {

    @NotEmpty(message = "At least one recipient ID is required")
    private List<Long> recipientIds;

    @NotNull
    private AlertType type;

    @Builder.Default
    private Severity severity = Severity.INFO;

    @NotBlank
    private String title;

    @NotBlank
    private String message;

    private Long   relatedProductId;
    private Long   relatedWarehouseId;
}
