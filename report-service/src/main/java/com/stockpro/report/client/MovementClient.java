package com.stockpro.report.client;

import com.stockpro.report.dto.external.MovementResponse;
import com.stockpro.report.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "movement-service", path = "/api/v1/movements")
public interface MovementClient {

    @GetMapping
    ApiResponse<List<MovementResponse>> getAllMovements();
}
