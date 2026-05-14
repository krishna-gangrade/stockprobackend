package com.stockpro.movement.client;

import com.stockpro.movement.dto.external.ProductResponse;
import com.stockpro.movement.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service", url = "${app.services.product-service.url:http://localhost:8085}", path = "/api/v1/products")
public interface ProductClient {

    @GetMapping("/{id}")
    ApiResponse<ProductResponse> getById(@PathVariable("id") Long id);
}
