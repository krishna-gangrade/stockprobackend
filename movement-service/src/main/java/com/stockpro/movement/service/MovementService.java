package com.stockpro.movement.service;

import com.stockpro.movement.dto.MovementRequest;
import com.stockpro.movement.dto.MovementResponse;
import com.stockpro.movement.entity.StockMovement.MovementType;

import java.time.LocalDateTime;
import java.util.List;

public interface MovementService {

    // Record a new movement — the only write operation (movements are immutable)
    MovementResponse recordMovement(MovementRequest request, Long performedById);

    // Read operations
    MovementResponse        getById(Long id);
    List<MovementResponse>  getAllMovements();
    List<MovementResponse>  getByProduct(Long productId);
    List<MovementResponse>  getByWarehouse(Long warehouseId);
    List<MovementResponse>  getByType(MovementType type);
    List<MovementResponse>  getByDateRange(LocalDateTime from, LocalDateTime to);
    List<MovementResponse>  getByReference(Long referenceId);
    List<MovementResponse>  getMovementHistory(Long productId, Long warehouseId);

    // Aggregates
    int getTotalStockIn(Long productId, Long warehouseId);
    int getTotalStockOut(Long productId, Long warehouseId);
}
