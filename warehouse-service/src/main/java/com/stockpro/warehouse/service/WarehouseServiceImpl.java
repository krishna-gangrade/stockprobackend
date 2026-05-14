package com.stockpro.warehouse.service;

import com.stockpro.warehouse.exception.ApiException;
import com.stockpro.warehouse.client.ProductClient;
import com.stockpro.warehouse.client.AlertClient;
import com.stockpro.warehouse.client.AuthClient;
import com.stockpro.warehouse.dto.external.ProductResponse;
import com.stockpro.warehouse.dto.external.AlertRequest;
import com.stockpro.warehouse.response.ApiResponse;
import com.stockpro.warehouse.dto.StockLevelResponse;
import com.stockpro.warehouse.dto.StockUpdateRequest;
import com.stockpro.warehouse.dto.TransferRequest;
import com.stockpro.warehouse.dto.WarehouseRequest;
import com.stockpro.warehouse.dto.WarehouseResponse;
import com.stockpro.warehouse.entity.StockLevel;
import com.stockpro.warehouse.entity.Warehouse;
import com.stockpro.warehouse.repository.StockLevelRepository;
import com.stockpro.warehouse.repository.WarehouseRepository;
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
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository  warehouseRepository;
    private final StockLevelRepository stockLevelRepository;
    private final ProductClient       productClient;
    private final AlertClient         alertClient;
    private final AuthClient          authClient;

    // =====================================================================
    // WAREHOUSE CRUD
    // =====================================================================

    @Override
    @Transactional
    @SuppressWarnings("null")
    public WarehouseResponse createWarehouse(WarehouseRequest request) {
        if (warehouseRepository.existsByName(request.getName())) {
            throw new ApiException("Warehouse with name '" + request.getName() + "' already exists",
                    HttpStatus.CONFLICT);
        }
        Warehouse warehouse = Warehouse.builder()
                .name(request.getName())
                .location(request.getLocation())
                .address(request.getAddress())
                .managerId(request.getManagerId())
                .capacity(request.getCapacity())
                .usedCapacity(0)
                .phone(request.getPhone())
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
        return mapToWarehouseResponse(warehouseRepository.save(warehouse));
    }

    @Override
    public WarehouseResponse getById(Long id) {
        return mapToWarehouseResponse(findWarehouseOrThrow(id));
    }

    @Override
    @SuppressWarnings("null")
    public boolean existsById(Long id) {
        return warehouseRepository.existsById(id);
    }

    @Override
    public List<WarehouseResponse> getAllWarehouses() {
        return warehouseRepository.findAll()
                .stream().map(this::mapToWarehouseResponse).collect(Collectors.toList());
    }

    @Override
    public List<WarehouseResponse> getActiveWarehouses() {
        return warehouseRepository.findByIsActive(true)
                .stream().map(this::mapToWarehouseResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public WarehouseResponse updateWarehouse(Long id, WarehouseRequest request) {
        Warehouse warehouse = findWarehouseOrThrow(id);
        warehouse.setName(request.getName());
        warehouse.setLocation(request.getLocation());
        warehouse.setAddress(request.getAddress());
        warehouse.setPhone(request.getPhone());
        warehouse.setCapacity(request.getCapacity());
        if (request.getManagerId() != null) {
            warehouse.setManagerId(request.getManagerId());
        }
        if (request.getIsActive() != null) {
            warehouse.setActive(request.getIsActive());
        }
        return mapToWarehouseResponse(warehouseRepository.save(warehouse));
    }

    @Override
    @Transactional
    public void deactivateWarehouse(Long id) {
        Warehouse warehouse = findWarehouseOrThrow(id);
        warehouse.setActive(false);
        warehouseRepository.save(warehouse);
    }

    @Override
    @Transactional
    public void assignManager(Long warehouseId, Long managerId) {
        Warehouse warehouse = findWarehouseOrThrow(warehouseId);
        warehouse.setManagerId(managerId);
        warehouseRepository.save(warehouse);
    }

    // =====================================================================
    // STOCK LEVEL OPERATIONS
    // =====================================================================

    @Override
    public StockLevelResponse getStockLevel(Long warehouseId, Long productId) {
        StockLevel stockLevel = stockLevelRepository
                .findByWarehouseIdAndProductId(warehouseId, productId)
                .orElseThrow(() -> new ApiException(
                        "No stock record found for product " + productId
                        + " in warehouse " + warehouseId, HttpStatus.NOT_FOUND));
        return mapToStockResponse(stockLevel);
    }

    @Override
    public List<StockLevelResponse> getStockByWarehouse(Long warehouseId) {
        findWarehouseOrThrow(warehouseId); // validate warehouse exists
        return stockLevelRepository.findByWarehouseId(warehouseId)
                .stream().map(this::mapToStockResponse).collect(Collectors.toList());
    }

    @Override
    public List<StockLevelResponse> getStockByProduct(Long productId) {
        return stockLevelRepository.findByProductId(productId)
                .stream().map(this::mapToStockResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public StockLevelResponse updateStock(StockUpdateRequest request) {
        log.info("Updating stock for product {} in warehouse {}: new quantity {}", 
                request.getProductId(), request.getWarehouseId(), request.getQuantity());
                
        Warehouse warehouse = findWarehouseOrThrow(request.getWarehouseId());
        findProductOrThrow(request.getProductId());

        // Get existing stock record or create a new one
        StockLevel stockLevel = stockLevelRepository
                .findByWarehouseIdAndProductId(request.getWarehouseId(), request.getProductId())
                .orElse(StockLevel.builder()
                        .warehouseId(request.getWarehouseId())
                        .productId(request.getProductId())
                        .reservedQuantity(0)
                        .build());

        validateWarehouseCapacity(warehouse, stockLevel.getQuantity(), request.getQuantity());

        stockLevel.setQuantity(request.getQuantity());
        stockLevel.setLastUpdated(LocalDateTime.now());
        if (request.getBinLocation() != null) {
            stockLevel.setBinLocation(request.getBinLocation());
        }

        StockLevel saved = stockLevelRepository.save(stockLevel);

        // Update warehouse used capacity after the new quantity is persisted.
        recalculateUsedCapacity(request.getWarehouseId());

        // Low Stock Check
        checkAndTriggerLowStockAlert(saved, warehouse);

        return mapToStockResponse(saved);
    }

    private void checkAndTriggerLowStockAlert(StockLevel stock, Warehouse warehouse) {
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                ProductResponse product = findProductOrThrow(stock.getProductId());
                int available = stock.getQuantity() - stock.getReservedQuantity();
                
                if (available < product.getReorderLevel()) {
                    log.info("Low stock detected for product {}: available={}, reorder={}", 
                            product.getName(), available, product.getReorderLevel());

                    String title = "Low Stock Alert: " + product.getName();
                    String message = String.format("Product %s (SKU: %s) is low in %s. Available: %d, Reorder Level: %d",
                            product.getName(), product.getSku(), warehouse.getName(), available, product.getReorderLevel());

                    java.util.Set<Long> recipients = new java.util.HashSet<>();
                    if (warehouse.getManagerId() != null) {
                        recipients.add(warehouse.getManagerId());
                    }

                    // Also notify all ADMINS
                    try {
                        log.info("ALERT DEBUG - Fetching ADMIN IDs from auth-service...");
                        ApiResponse<List<Long>> adminResponse = authClient.getUserIdsByRole("ADMIN");
                        if (adminResponse != null && adminResponse.isSuccess() && adminResponse.getData() != null) {
                            log.info("ALERT DEBUG - Auth service returned {} admins: {}", adminResponse.getData().size(), adminResponse.getData());
                            recipients.addAll(adminResponse.getData());
                        } else {
                            log.warn("ALERT DEBUG - Auth service returned no admins or failed. Response: {}", adminResponse);
                        }
                    } catch (Exception e) {
                        log.warn("ALERT DEBUG - Failed to fetch ADMIN IDs: {}. This might be a connection issue.", e.getMessage());
                    }

                    // FALLBACK: If still no recipients, try hardcoded ID 1 (Administrator)
                    if (recipients.isEmpty()) {
                        log.info("ALERT DEBUG - No recipients found. Using fallback Admin ID 1.");
                        recipients.add(1L);
                    }

                    if (!recipients.isEmpty()) {
                        log.info("ALERT DEBUG - Found {} total recipients for low stock alert. Sending bulk alert...", recipients.size());
                        com.stockpro.warehouse.dto.external.BulkAlertRequest bulkRequest = com.stockpro.warehouse.dto.external.BulkAlertRequest.builder()
                                .recipientIds(new java.util.ArrayList<>(recipients))
                                .type(AlertRequest.AlertType.LOW_STOCK)
                                .severity(available <= 0 ? AlertRequest.Severity.CRITICAL : AlertRequest.Severity.WARNING)
                                .title(title)
                                .message(message)
                                .relatedProductId(product.getId())
                                .relatedWarehouseId(warehouse.getId())
                                .build();

                        alertClient.sendBulkAlert(bulkRequest);
                        log.info("Low stock alerts sent successfully to {} recipients for product {}", 
                                recipients.size(), product.getName());
                    } else {
                        log.warn("ALERT DEBUG - No recipients found (No Manager and No Admins)! Skipping alert.");
                    }
                }
            } catch (Exception e) {
                log.error("Failed to trigger low stock alert asynchronously: {}", e.getMessage());
            }
        });
    }

    // =====================================================================
    // RESERVE & RELEASE (called by PurchaseOrder service)
    // =====================================================================

    @Override
    @Transactional
    public void reserveStock(Long warehouseId, Long productId, int quantity) {
        StockLevel stockLevel = stockLevelRepository
                .findByWarehouseIdAndProductId(warehouseId, productId)
                .orElseThrow(() -> new ApiException(
                        "No stock found for product " + productId
                        + " in warehouse " + warehouseId, HttpStatus.NOT_FOUND));

        // Cannot reserve more than what is available
        int available = stockLevel.getAvailableQuantity();
        if (quantity > available) {
            throw new ApiException(
                    "Insufficient stock. Available: " + available + ", Requested: " + quantity);
        }

        stockLevel.setReservedQuantity(stockLevel.getReservedQuantity() + quantity);
        stockLevel.setLastUpdated(LocalDateTime.now());
        stockLevelRepository.save(stockLevel);
    }

    @Override
    @Transactional
    public void releaseReservation(Long warehouseId, Long productId, int quantity) {
        StockLevel stockLevel = stockLevelRepository
                .findByWarehouseIdAndProductId(warehouseId, productId)
                .orElseThrow(() -> new ApiException(
                        "No stock found for product " + productId
                        + " in warehouse " + warehouseId, HttpStatus.NOT_FOUND));

        int newReserved = Math.max(0, stockLevel.getReservedQuantity() - quantity);
        stockLevel.setReservedQuantity(newReserved);
        stockLevel.setLastUpdated(LocalDateTime.now());
        stockLevelRepository.save(stockLevel);
    }

    // =====================================================================
    // INTER-WAREHOUSE TRANSFER
    // =====================================================================

    @Override
    @Transactional
    public void transferStock(TransferRequest request) {
        if (request.getFromWarehouseId().equals(request.getToWarehouseId())) {
            throw new ApiException("Source and destination warehouses cannot be the same");
        }

        findWarehouseOrThrow(request.getFromWarehouseId());
        findWarehouseOrThrow(request.getToWarehouseId());
        findProductOrThrow(request.getProductId());

        StockLevel source = stockLevelRepository
                .findByWarehouseIdAndProductId(
                        request.getFromWarehouseId(), request.getProductId())
                .orElseThrow(() -> new ApiException(
                        "No stock found for this product in source warehouse",
                        HttpStatus.NOT_FOUND));

        if (source.getAvailableQuantity() < request.getQuantity()) {
            throw new ApiException(
                    "Insufficient available stock in source warehouse. " +
                    "Available: " + source.getAvailableQuantity() +
                    ", Requested: " + request.getQuantity());
        }

        Warehouse destinationWarehouse = findWarehouseOrThrow(request.getToWarehouseId());

        StockLevel destination = stockLevelRepository
                .findByWarehouseIdAndProductId(
                        request.getToWarehouseId(), request.getProductId())
                .orElse(StockLevel.builder()
                        .warehouseId(request.getToWarehouseId())
                        .productId(request.getProductId())
                        .quantity(0)
                        .reservedQuantity(0)
                        .build());

        validateWarehouseCapacity(
                destinationWarehouse,
                destination.getQuantity(),
                destination.getQuantity() + request.getQuantity());

        source.setQuantity(source.getQuantity() - request.getQuantity());
        source.setLastUpdated(LocalDateTime.now());
        stockLevelRepository.save(source);

        destination.setQuantity(destination.getQuantity() + request.getQuantity());
        destination.setLastUpdated(LocalDateTime.now());
        stockLevelRepository.save(destination);

        recalculateUsedCapacity(request.getFromWarehouseId());
        recalculateUsedCapacity(request.getToWarehouseId());
    }

    @Override
    public List<StockLevelResponse> getLowStockItems() {
        ApiResponse<java.util.List<ProductResponse>> productResponse = productClient.getAllProducts();
        if (productResponse == null || !productResponse.isSuccess() || productResponse.getData() == null) {
            return java.util.Collections.emptyList();
        }
        
        java.util.Map<Long, ProductResponse> productMap = productResponse.getData().stream()
            .filter(ProductResponse::isActive)
            .collect(Collectors.toMap(ProductResponse::getId, p -> p));
            
        return stockLevelRepository.findAll().stream()
            .filter(sl -> {
                ProductResponse p = productMap.get(sl.getProductId());
                if (p == null) return false;
                return (sl.getQuantity() - sl.getReservedQuantity()) < p.getReorderLevel();
            })
            .map(this::mapToStockResponse)
            .collect(Collectors.toList());
    }

    @Override
    public List<StockLevelResponse> getOverstockItems() {
        ApiResponse<java.util.List<ProductResponse>> productResponse = productClient.getAllProducts();
        if (productResponse == null || !productResponse.isSuccess() || productResponse.getData() == null) {
            return java.util.Collections.emptyList();
        }
        
        java.util.Map<Long, ProductResponse> productMap = productResponse.getData().stream()
            .filter(ProductResponse::isActive)
            .collect(Collectors.toMap(ProductResponse::getId, p -> p));
            
        return stockLevelRepository.findAll().stream()
            .filter(sl -> {
                ProductResponse p = productMap.get(sl.getProductId());
                if (p == null) return false;
                return sl.getQuantity() > p.getMaxStockLevel();
            })
            .map(this::mapToStockResponse)
            .collect(Collectors.toList());
    }

    @SuppressWarnings("null")
    private Warehouse findWarehouseOrThrow(Long id) {
        return warehouseRepository.findById(id)
                .orElseThrow(() -> new ApiException(
                        "Warehouse not found with id: " + id, HttpStatus.NOT_FOUND));
    }

    private ProductResponse findProductOrThrow(Long id) {
        ApiResponse<ProductResponse> response = productClient.getById(id);
        if (response == null || !response.isSuccess() || response.getData() == null) {
            throw new ApiException("Product not found with id: " + id, HttpStatus.NOT_FOUND);
        }
        return response.getData();
    }

    private void validateWarehouseCapacity(Warehouse warehouse, int currentProductQty, int newProductQty) {
        int projectedUsedCapacity = stockLevelRepository.findByWarehouseId(warehouse.getId())
                .stream()
                .mapToInt(StockLevel::getQuantity)
                .sum() - currentProductQty + newProductQty;

        if (projectedUsedCapacity > warehouse.getCapacity()) {
            int availableSpace = Math.max(0, warehouse.getCapacity()
                    - (projectedUsedCapacity - newProductQty));
            throw new ApiException(
                    "Not enough warehouse capacity. Available: " + availableSpace
                    + ", Requested: " + newProductQty
                    + ", Capacity: " + warehouse.getCapacity(),
                    HttpStatus.BAD_REQUEST);
        }
    }

    private void recalculateUsedCapacity(Long warehouseId) {
        int total = stockLevelRepository.findByWarehouseId(warehouseId)
                .stream().mapToInt(StockLevel::getQuantity).sum();
        Warehouse warehouse = findWarehouseOrThrow(warehouseId);
        warehouse.setUsedCapacity(total);
        warehouseRepository.save(warehouse);
    }

    private WarehouseResponse mapToWarehouseResponse(Warehouse w) {
        return WarehouseResponse.builder()
                .id(w.getId())
                .name(w.getName())
                .location(w.getLocation())
                .address(w.getAddress())
                .managerId(w.getManagerId())
                .capacity(w.getCapacity())
                .usedCapacity(w.getUsedCapacity())
                .phone(w.getPhone())
                .isActive(w.isActive())
                .createdAt(w.getCreatedAt())
                .build();
    }

    @SuppressWarnings("null")
    private StockLevelResponse mapToStockResponse(StockLevel sl) {
        ProductResponse product = null;
        try {
            product = findProductOrThrow(sl.getProductId());
        } catch (Exception e) {}
        
        String productName = (product != null) ? product.getName() : "Unknown";
        String productSku = (product != null) ? product.getSku() : "N/A";
        String warehouseName = warehouseRepository.findById(sl.getWarehouseId())
                .map(Warehouse::getName).orElse("Unknown");

        return StockLevelResponse.builder()
                .id(sl.getId())
                .warehouseId(sl.getWarehouseId())
                .warehouseName(warehouseName)
                .productId(sl.getProductId())
                .productName(productName)
                .productSku(productSku)
                .quantity(sl.getQuantity())
                .reservedQuantity(sl.getReservedQuantity())
                .availableQuantity(sl.getAvailableQuantity())
                .binLocation(sl.getBinLocation())
                .lastUpdated(sl.getLastUpdated())
                .build();
    }
}
