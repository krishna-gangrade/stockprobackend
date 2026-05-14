package com.stockpro.movement.service;

import com.stockpro.movement.dto.MovementRequest;
import com.stockpro.movement.dto.MovementResponse;
import com.stockpro.movement.entity.StockMovement;
import com.stockpro.movement.entity.StockMovement.MovementType;
import com.stockpro.movement.repository.MovementRepository;
import com.stockpro.movement.client.ProductClient;
import com.stockpro.movement.client.WarehouseClient;
import com.stockpro.movement.kafka.MovementProducer;
import com.stockpro.movement.kafka.StockUpdateEvent;
import com.stockpro.movement.response.ApiResponse;
import com.stockpro.movement.dto.external.StockLevelResponse;
import com.stockpro.movement.dto.external.StockUpdateRequest;
import com.stockpro.movement.dto.external.TransferRequest;
import com.stockpro.movement.dto.external.ProductResponse;
import com.stockpro.movement.dto.external.WarehouseResponse;
import com.stockpro.movement.exception.ApiException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MovementServiceImpl implements MovementService {

    private final MovementRepository movementRepository;
    private final ProductClient      productClient;
    private final WarehouseClient    warehouseClient;
    private final MovementProducer   movementProducer;

    @Override
    @Transactional
    @SuppressWarnings("null")
    public MovementResponse recordMovement(MovementRequest request, Long performedById) {
        if (isTransfer(request.getMovementType())) {
            return recordTransferMovement(request, performedById);
        }

        // Validate product exists
        ApiResponse<?> productResponse = productClient.getById(request.getProductId());
        if (productResponse == null || !productResponse.isSuccess()) {
            throw new ApiException("Product not found with id: " + request.getProductId(), HttpStatus.NOT_FOUND);
        }
 
        // Validate warehouse exists
        ApiResponse<?> warehouseResponse = warehouseClient.getById(request.getWarehouseId());
        if (warehouseResponse == null || !warehouseResponse.isSuccess()) {
            throw new ApiException("Warehouse not found with id: " + request.getWarehouseId(), HttpStatus.NOT_FOUND);
        }

        // Fetch current stock level from warehouse-service
        int currentQty = getCurrentStockQuantity(request);

        // Calculate balance after this movement
        int balanceAfter = calculateBalanceAfter(currentQty, request.getMovementType(), request.getQuantity(), request.getReferenceType());

        if (balanceAfter < 0) {
            throw new ApiException("Movement would result in negative stock.", HttpStatus.BAD_REQUEST);
        }

        // Build and save the immutable movement record
        StockMovement movement = StockMovement.builder()
                .productId(request.getProductId())
                .warehouseId(request.getWarehouseId())
                .fromWarehouseId(request.getWarehouseId())
                .toWarehouseId(request.getWarehouseId())
                .movementType(request.getMovementType())
                .quantity(request.getQuantity())
                .unitCost(request.getUnitCost())
                .referenceId(request.getReferenceId())
                .referenceType(request.getReferenceType())
                .performedBy(performedById)
                .notes(request.getNotes())
                .balanceAfter(balanceAfter)
                .movementDate(LocalDateTime.now())
                .build();

        StockMovement saved = movementRepository.save(movement);

        warehouseClient.updateStock(StockUpdateRequest.builder()
                .productId(saved.getProductId())
                .warehouseId(saved.getWarehouseId())
                .quantity(saved.getBalanceAfter())
                .build());

        // Publish Kafka event so warehouse-service can update its stock levels
        movementProducer.sendStockUpdate(StockUpdateEvent.builder()
                .productId(saved.getProductId())
                .warehouseId(saved.getWarehouseId())
                .quantity(saved.getQuantity())
                .movementType(saved.getMovementType())
                .movementId(saved.getId())
                .build());

        log.info("Movement recorded and event published: {} units of product {} in warehouse {}", 
                request.getQuantity(), request.getProductId(), request.getWarehouseId());

        return mapToResponse(saved);
    }

    @SuppressWarnings("null")
    private MovementResponse recordTransferMovement(MovementRequest request, Long performedById) {
        Long fromWarehouseId = request.getFromWarehouseId();
        Long toWarehouseId = request.getToWarehouseId();

        if (fromWarehouseId == null || toWarehouseId == null) {
            throw new ApiException("Transfer requires both source and destination warehouses", HttpStatus.BAD_REQUEST);
        }
        if (fromWarehouseId.equals(toWarehouseId)) {
            throw new ApiException("Source and destination warehouses cannot be the same", HttpStatus.BAD_REQUEST);
        }

        ApiResponse<?> productResponse = productClient.getById(request.getProductId());
        if (productResponse == null || !productResponse.isSuccess()) {
            throw new ApiException("Product not found with id: " + request.getProductId(), HttpStatus.NOT_FOUND);
        }

        validateWarehouse(fromWarehouseId);
        validateWarehouse(toWarehouseId);

        int sourceQty = getCurrentStockQuantity(fromWarehouseId, request.getProductId());
        int sourceBalanceAfter = sourceQty - request.getQuantity();
        if (sourceBalanceAfter < 0) {
            throw new ApiException("Transfer would result in negative stock in source warehouse.", HttpStatus.BAD_REQUEST);
        }

        int destinationQty = getCurrentStockQuantity(toWarehouseId, request.getProductId());
        int destinationBalanceAfter = destinationQty + request.getQuantity();
        LocalDateTime movementDate = LocalDateTime.now();
        String referenceType = request.getReferenceType() != null && !request.getReferenceType().isBlank()
                ? request.getReferenceType()
                : "TRANSFER";

        StockMovement transferOut = StockMovement.builder()
                .productId(request.getProductId())
                .warehouseId(fromWarehouseId)
                .fromWarehouseId(fromWarehouseId)
                .toWarehouseId(toWarehouseId)
                .movementType(MovementType.TRANSFER_OUT)
                .quantity(request.getQuantity())
                .unitCost(request.getUnitCost())
                .referenceId(request.getReferenceId())
                .referenceType(referenceType)
                .performedBy(performedById)
                .notes(request.getNotes())
                .balanceAfter(sourceBalanceAfter)
                .movementDate(movementDate)
                .build();

        StockMovement transferIn = StockMovement.builder()
                .productId(request.getProductId())
                .warehouseId(toWarehouseId)
                .fromWarehouseId(fromWarehouseId)
                .toWarehouseId(toWarehouseId)
                .movementType(MovementType.TRANSFER_IN)
                .quantity(request.getQuantity())
                .unitCost(request.getUnitCost())
                .referenceId(request.getReferenceId())
                .referenceType(referenceType)
                .performedBy(performedById)
                .notes(request.getNotes())
                .balanceAfter(destinationBalanceAfter)
                .movementDate(movementDate)
                .build();

        StockMovement savedTransferOut = movementRepository.save(transferOut);
        movementRepository.save(transferIn);

        warehouseClient.transferStock(TransferRequest.builder()
                .productId(request.getProductId())
                .fromWarehouseId(fromWarehouseId)
                .toWarehouseId(toWarehouseId)
                .quantity(request.getQuantity())
                .reason(request.getNotes())
                .build());

        log.info("Transfer recorded: {} units of product {} from warehouse {} to warehouse {}",
                request.getQuantity(), request.getProductId(), fromWarehouseId, toWarehouseId);

        return mapToResponse(savedTransferOut);
    }

    @Override
    @SuppressWarnings("null")
    public MovementResponse getById(Long id) {
        StockMovement movement = movementRepository.findById(id)
                .orElseThrow(() -> new ApiException("Movement not found: " + id, HttpStatus.NOT_FOUND));
        return mapToResponse(movement);
    }

    @Override
    public List<MovementResponse> getAllMovements() {
        return movementRepository.findAll().stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<MovementResponse> getByProduct(Long productId) {
        return movementRepository.findByProductIdOrderByMovementDateDesc(productId).stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<MovementResponse> getByWarehouse(Long warehouseId) {
        return movementRepository.findByWarehouseId(warehouseId).stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<MovementResponse> getByType(MovementType type) {
        return movementRepository.findByMovementType(type).stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<MovementResponse> getByDateRange(LocalDateTime from, LocalDateTime to) {
        return movementRepository.findByMovementDateBetween(from, to).stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<MovementResponse> getByReference(Long referenceId) {
        return movementRepository.findByReferenceId(referenceId).stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<MovementResponse> getMovementHistory(Long productId, Long warehouseId) {
        return movementRepository.findByProductIdAndWarehouseIdOrderByMovementDateAsc(productId, warehouseId).stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public int getTotalStockIn(Long productId, Long warehouseId) {
        Integer sum = movementRepository.sumStockIn(productId, warehouseId);
        return sum != null ? sum : 0;
    }

    @Override
    public int getTotalStockOut(Long productId, Long warehouseId) {
        Integer sum = movementRepository.sumStockOut(productId, warehouseId);
        return sum != null ? sum : 0;
    }

    private int calculateBalanceAfter(int currentQty, MovementType type, int qty, String referenceType) {
        return switch (type) {
            case STOCK_IN, TRANSFER_IN, ADJUSTMENT -> currentQty + qty;
            case STOCK_OUT, TRANSFER_OUT, WRITE_OFF -> currentQty - qty;
            case RETURN -> isSupplierReturn(referenceType)
                    ? currentQty - qty
                    : currentQty + qty;
        };
    }

    private int getCurrentStockQuantity(MovementRequest request) {
        return getCurrentStockQuantity(request.getWarehouseId(), request.getProductId());
    }

    private int getCurrentStockQuantity(Long warehouseId, Long productId) {
        try {
            ApiResponse<StockLevelResponse> stockResponse =
                    warehouseClient.getStockLevel(warehouseId, productId);
            return (stockResponse != null && stockResponse.isSuccess() && stockResponse.getData() != null)
                    ? stockResponse.getData().getQuantity()
                    : 0;
        } catch (FeignException.NotFound ex) {
            return 0;
        }
    }

    private MovementResponse mapToResponse(StockMovement m) {
        ProductResponse product = getProduct(m.getProductId());
        WarehouseResponse warehouse = getWarehouse(m.getWarehouseId());
        WarehouseResponse fromWarehouse = getWarehouse(m.getFromWarehouseId());
        WarehouseResponse toWarehouse = getWarehouse(m.getToWarehouseId());

        return MovementResponse.builder()
                .id(m.getId())
                .productId(m.getProductId())
                .productName(product != null ? product.getName() : null)
                .productSku(product != null ? product.getSku() : null)
                .warehouseId(m.getWarehouseId())
                .warehouseName(warehouse != null ? warehouse.getName() : null)
                .fromWarehouseId(m.getFromWarehouseId())
                .fromWarehouseName(fromWarehouse != null ? fromWarehouse.getName() : null)
                .toWarehouseId(m.getToWarehouseId())
                .toWarehouseName(toWarehouse != null ? toWarehouse.getName() : null)
                .movementType(m.getMovementType())
                .quantity(m.getQuantity())
                .unitCost(m.getUnitCost())
                .referenceId(m.getReferenceId())
                .referenceType(m.getReferenceType())
                .performedBy(m.getPerformedBy())
                .notes(m.getNotes())
                .balanceAfter(m.getBalanceAfter())
                .movementDate(m.getMovementDate())
                .build();
    }

    private boolean isTransfer(MovementType movementType) {
        return movementType == MovementType.TRANSFER_IN || movementType == MovementType.TRANSFER_OUT;
    }

    private boolean isSupplierReturn(String referenceType) {
        return "SUPPLIER_RETURN".equalsIgnoreCase(referenceType);
    }

    private void validateWarehouse(Long warehouseId) {
        ApiResponse<WarehouseResponse> warehouseResponse = warehouseClient.getById(warehouseId);
        if (warehouseResponse == null || !warehouseResponse.isSuccess()) {
            throw new ApiException("Warehouse not found with id: " + warehouseId, HttpStatus.NOT_FOUND);
        }
    }

    private ProductResponse getProduct(Long productId) {
        try {
            ApiResponse<ProductResponse> response = productClient.getById(productId);
            return response != null && response.isSuccess() ? response.getData() : null;
        } catch (Exception ex) {
            log.warn("Could not load product {}", productId, ex);
            return null;
        }
    }

    private WarehouseResponse getWarehouse(Long warehouseId) {
        if (warehouseId == null) {
            return null;
        }
        try {
            ApiResponse<WarehouseResponse> response = warehouseClient.getById(warehouseId);
            return response != null && response.isSuccess() ? response.getData() : null;
        } catch (Exception ex) {
            log.warn("Could not load warehouse {}", warehouseId, ex);
            return null;
        }
    }
}
