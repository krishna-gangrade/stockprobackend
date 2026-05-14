package com.stockpro.report.client;

import com.stockpro.report.dto.external.PurchaseOrderResponse;
import com.stockpro.report.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "purchase-service", path = "/api/v1/purchase-orders")
public interface PurchaseClient {

    @GetMapping("/date-range")
    ApiResponse<List<PurchaseOrderResponse>> getByDateRange(
            @RequestParam("from") String from,
            @RequestParam("to") String to);
}
