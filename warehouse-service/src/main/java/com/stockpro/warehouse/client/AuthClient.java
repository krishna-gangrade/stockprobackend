package com.stockpro.warehouse.client;

import com.stockpro.warehouse.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "auth-service", url = "${app.services.auth-service.url:http://localhost:8083/api/v1}")
public interface AuthClient {

    @GetMapping("/auth/api/users/ids-by-role")
    ApiResponse<List<Long>> getUserIdsByRole(@RequestParam("role") String role);
}
