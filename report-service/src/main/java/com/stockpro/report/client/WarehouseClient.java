package com.stockpro.report.client;

import com.stockpro.report.dto.external.StockLevelResponse;
import com.stockpro.report.dto.external.WarehouseResponse;
import com.stockpro.report.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "warehouse-service", path = "/api/v1/warehouses")
public interface WarehouseClient {

    @GetMapping
    ApiResponse<List<WarehouseResponse>> getAllWarehouses();

    @GetMapping("/{warehouseId}/stock")
    ApiResponse<List<StockLevelResponse>> getStockByWarehouse(@PathVariable("warehouseId") Long warehouseId);
}
