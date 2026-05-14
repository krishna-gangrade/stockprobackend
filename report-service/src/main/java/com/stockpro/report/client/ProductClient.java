package com.stockpro.report.client;

import com.stockpro.report.dto.external.ProductResponse;
import com.stockpro.report.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "product-service", path = "/api/v1/products")
public interface ProductClient {

    @GetMapping("/{id}")
    ApiResponse<ProductResponse> getById(@PathVariable("id") Long id);

    @GetMapping
    ApiResponse<List<ProductResponse>> getAllProducts();
}
