package com.stockpro.warehouse.client;

import com.stockpro.warehouse.dto.external.ProductResponse;
import com.stockpro.warehouse.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service", path = "/api/v1/products")
public interface ProductClient {

    @GetMapping("/{id}")
    ApiResponse<ProductResponse> getById(@PathVariable("id") Long id);

    @GetMapping
    ApiResponse<java.util.List<ProductResponse>> getAllProducts();
}
