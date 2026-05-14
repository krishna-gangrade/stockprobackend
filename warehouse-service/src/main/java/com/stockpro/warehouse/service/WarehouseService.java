package com.stockpro.warehouse.service;

import com.stockpro.warehouse.dto.*;

import java.util.List;

public interface WarehouseService {

    // ===== Warehouse CRUD =====
    WarehouseResponse        createWarehouse(WarehouseRequest request);
    WarehouseResponse        getById(Long id);
    boolean                  existsById(Long id);
    List<WarehouseResponse>  getAllWarehouses();
    List<WarehouseResponse>  getActiveWarehouses();
    WarehouseResponse        updateWarehouse(Long id, WarehouseRequest request);
    void                     deactivateWarehouse(Long id);
    void                     assignManager(Long warehouseId, Long managerId);

    // ===== Stock Level Operations =====
    StockLevelResponse       getStockLevel(Long warehouseId, Long productId);
    List<StockLevelResponse> getStockByWarehouse(Long warehouseId);
    List<StockLevelResponse> getStockByProduct(Long productId);
    StockLevelResponse       updateStock(StockUpdateRequest request);

    // ===== Reserve & Release (used by Purchase Order service) =====
    void reserveStock(Long warehouseId, Long productId, int quantity);
    void releaseReservation(Long warehouseId, Long productId, int quantity);

    // ===== Inter-Warehouse Transfer =====
    void transferStock(TransferRequest request);

    // ===== Low Stock Query (used by Alert scheduler) =====
    List<StockLevelResponse> getLowStockItems();
    List<StockLevelResponse> getOverstockItems();
}
