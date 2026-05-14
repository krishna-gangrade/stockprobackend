package com.stockpro.warehouse.client;

import com.stockpro.warehouse.dto.external.AlertRequest;
import com.stockpro.warehouse.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "alert-service", url = "${app.services.alert-service.url:http://localhost:8087/api/v1}")
public interface AlertClient {

    @PostMapping("/alerts")
    ApiResponse<?> sendAlert(@RequestBody AlertRequest request);

    @PostMapping("/alerts/bulk")
    ApiResponse<?> sendBulkAlert(@RequestBody com.stockpro.warehouse.dto.external.BulkAlertRequest request);
}
