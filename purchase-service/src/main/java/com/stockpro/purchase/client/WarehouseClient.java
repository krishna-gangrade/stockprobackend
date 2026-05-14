package com.stockpro.purchase.client;

import com.stockpro.purchase.dto.external.StockLevelResponse;
import com.stockpro.purchase.dto.external.StockUpdateRequest;
import com.stockpro.purchase.dto.external.WarehouseResponse;
import com.stockpro.purchase.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;


@FeignClient(name = "warehouse-service", path = "/api/v1/warehouses")
public interface WarehouseClient {

    @GetMapping("/{id}")
    ApiResponse<WarehouseResponse> getById(@PathVariable("id") Long id);

    @GetMapping("/exists/{id}")
    ApiResponse<Boolean> existsById(@PathVariable("id") Long id);

    @GetMapping("/{warehouseId}/stock/{productId}")
    ApiResponse<StockLevelResponse> getStockLevel(@PathVariable("warehouseId") Long warehouseId, 
                                                 @PathVariable("productId") Long productId);

    @PutMapping("/stock")
    ApiResponse<StockLevelResponse> updateStock(@RequestBody StockUpdateRequest request);
}
