package com.stockpro.movement.client;

import com.stockpro.movement.dto.external.StockLevelResponse;
import com.stockpro.movement.dto.external.StockUpdateRequest;
import com.stockpro.movement.dto.external.TransferRequest;
import com.stockpro.movement.dto.external.WarehouseResponse;
import com.stockpro.movement.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "warehouse-service", path = "/api/v1/warehouses")
public interface WarehouseClient {

    @GetMapping("/{id}")
    ApiResponse<WarehouseResponse> getById(@PathVariable("id") Long id);

    @GetMapping("/{warehouseId}/stock/{productId}")
    ApiResponse<StockLevelResponse> getStockLevel(
            @PathVariable("warehouseId") Long warehouseId,
            @PathVariable("productId") Long productId);

    @PutMapping("/stock")
    ApiResponse<StockLevelResponse> updateStock(@RequestBody StockUpdateRequest request);

    @PostMapping("/stock/transfer")
    ApiResponse<Void> transferStock(@RequestBody TransferRequest request);
}
