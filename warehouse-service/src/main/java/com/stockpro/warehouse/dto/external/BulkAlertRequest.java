package com.stockpro.warehouse.dto.external;

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
    private List<Long> recipientIds;
    private AlertRequest.AlertType type;
    private AlertRequest.Severity severity;
    private String title;
    private String message;
    private Long relatedProductId;
    private Long relatedWarehouseId;
    private String channel;
}
