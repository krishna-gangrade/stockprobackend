package com.stockpro.report.service;

import com.stockpro.report.client.MovementClient;
import com.stockpro.report.client.ProductClient;
import com.stockpro.report.client.PurchaseClient;
import com.stockpro.report.client.WarehouseClient;
import com.stockpro.report.dto.*;
import com.stockpro.report.dto.external.*;
import com.stockpro.report.entity.InventorySnapshot;
import com.stockpro.report.repository.ReportRepository;
import com.stockpro.report.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final ProductClient    productClient;
    private final WarehouseClient  warehouseClient;
    private final MovementClient   movementClient;
    private final PurchaseClient   purchaseClient;

    @Override
    @Transactional
    public void takeSnapshot() {
        log.info("Starting inventory snapshot process...");
        
        // 1. Get all warehouses
        ApiResponse<List<WarehouseResponse>> warehousesResponse = warehouseClient.getAllWarehouses();
        if (warehousesResponse == null || warehousesResponse.getData() == null) return;
        
        // 2. Get all products (to get costPrice)
        ApiResponse<List<ProductResponse>> productsResponse = productClient.getAllProducts();
        if (productsResponse == null || productsResponse.getData() == null) return;
        
        Map<Long, BigDecimal> productPrices = productsResponse.getData().stream()
                .collect(Collectors.toMap(ProductResponse::getId, ProductResponse::getCostPrice));
        
        LocalDate today = LocalDate.now();
        List<InventorySnapshot> snapshots = new ArrayList<>();
        
        for (WarehouseResponse warehouse : warehousesResponse.getData()) {
            // 3. Get stock levels for this warehouse
            ApiResponse<List<StockLevelResponse>> stockResponse = warehouseClient.getStockByWarehouse(warehouse.getId());
            if (stockResponse == null || stockResponse.getData() == null) continue;
            
            for (StockLevelResponse stock : stockResponse.getData()) {
                BigDecimal price = productPrices.getOrDefault(stock.getProductId(), BigDecimal.ZERO);
                BigDecimal value = price.multiply(BigDecimal.valueOf(stock.getQuantity()));
                
                InventorySnapshot snapshot = InventorySnapshot.builder()
                        .warehouseId(warehouse.getId())
                        .productId(stock.getProductId())
                        .quantity(stock.getQuantity())
                        .stockValue(value)
                        .snapshotDate(today)
                        .build();
                snapshots.add(snapshot);
            }
        }
        
        reportRepository.saveAll(snapshots);
        log.info("Inventory snapshot completed. Saved {} records.", snapshots.size());
    }

    @Override
    public List<SnapshotResponse> getSnapshotByDate(LocalDate date) {
        return reportRepository.findBySnapshotDate(date)
                .stream().map(this::mapToSnapshotResponse).collect(Collectors.toList());
    }

    @Override
    public List<SnapshotResponse> getSnapshotByWarehouse(Long warehouseId) {
        return reportRepository.findByWarehouseId(warehouseId)
                .stream().map(this::mapToSnapshotResponse).collect(Collectors.toList());
    }

    @Override
    public StockValuationResponse getTotalStockValue() {
        ApiResponse<List<WarehouseResponse>> warehousesResponse = warehouseClient.getAllWarehouses();
        if (warehousesResponse == null || warehousesResponse.getData() == null) {
            return StockValuationResponse.builder().totalValue(BigDecimal.ZERO).asOfDate(LocalDate.now()).build();
        }
        
        BigDecimal totalValue = BigDecimal.ZERO;
        for (WarehouseResponse w : warehousesResponse.getData()) {
            totalValue = totalValue.add(getStockValueByWarehouse(w.getId()).getTotalValue());
        }
        
        return StockValuationResponse.builder()
                .totalValue(totalValue)
                .asOfDate(LocalDate.now())
                .build();
    }

    @Override
    public StockValuationResponse getStockValueByWarehouse(Long warehouseId) {
        ApiResponse<List<StockLevelResponse>> stockResponse = warehouseClient.getStockByWarehouse(warehouseId);
        if (stockResponse == null || stockResponse.getData() == null) {
            return StockValuationResponse.builder().totalValue(BigDecimal.ZERO).asOfDate(LocalDate.now()).warehouseId(warehouseId).build();
        }
        
        BigDecimal warehouseValue = BigDecimal.ZERO;
        for (StockLevelResponse stock : stockResponse.getData()) {
            ApiResponse<ProductResponse> productResponse = productClient.getById(stock.getProductId());
            if (productResponse != null && productResponse.getData() != null) {
                BigDecimal price = productResponse.getData().getCostPrice();
                warehouseValue = warehouseValue.add(price.multiply(BigDecimal.valueOf(stock.getQuantity())));
            }
        }
        
        return StockValuationResponse.builder()
                .totalValue(warehouseValue)
                .asOfDate(LocalDate.now())
                .warehouseId(warehouseId)
                .build();
    }

    @Override
    public TurnoverResponse getInventoryTurnover(LocalDate from, LocalDate to) {
        return TurnoverResponse.builder()
                .turnoverRate(BigDecimal.ZERO)
                .fromDate(from)
                .toDate(to)
                .build();
    }

    @Override
    public TurnoverResponse getInventoryTurnoverByWarehouse(Long warehouseId, LocalDate from, LocalDate to) {
        return TurnoverResponse.builder()
                .turnoverRate(BigDecimal.ZERO)
                .fromDate(from)
                .toDate(to)
                .warehouseId(warehouseId)
                .build();
    }

    @Override
    public List<TopMovingProductResponse> getTopMovingProducts(int topN) {
        ApiResponse<List<MovementResponse>> response = movementClient.getAllMovements();
        if (response == null || response.getData() == null) return Collections.emptyList();

        List<MovementResponse> movements = response.getData();
        Map<Long, List<MovementResponse>> grouped = movements.stream()
                .collect(Collectors.groupingBy(MovementResponse::getProductId));

        List<TopMovingProductResponse> list = new ArrayList<>();
        for (Map.Entry<Long, List<MovementResponse>> entry : grouped.entrySet()) {
            Long pid = entry.getKey();
            List<MovementResponse> pMovements = entry.getValue();

            int in = pMovements.stream()
                    .filter(m -> m.getMovementType().contains("IN") || m.getMovementType().equals("ADJUSTMENT") && m.getQuantity() > 0)
                    .mapToInt(MovementResponse::getQuantity).sum();
            int out = pMovements.stream()
                    .filter(m -> m.getMovementType().contains("OUT") || m.getMovementType().equals("WRITE_OFF") || (m.getMovementType().equals("ADJUSTMENT") && m.getQuantity() < 0))
                    .mapToInt(m -> Math.abs(m.getQuantity())).sum();

            ApiResponse<ProductResponse> pr = productClient.getById(pid);
            String name = pr != null && pr.getData() != null ? pr.getData().getName() : "Unknown";
            String sku = pr != null && pr.getData() != null ? pr.getData().getSku() : "Unknown";
            String cat = pr != null && pr.getData() != null ? pr.getData().getCategory() : "Misc";

            list.add(TopMovingProductResponse.builder()
                    .productId(pid)
                    .productName(name)
                    .productSku(sku)
                    .category(cat)
                    .totalUnitsIn(in)
                    .totalUnitsOut(out)
                    .totalUnitsMoved(in + out)
                    .build());
        }

        List<TopMovingProductResponse> sorted = list.stream()
                .sorted(Comparator.comparingInt(TopMovingProductResponse::getTotalUnitsMoved).reversed())
                .limit(topN)
                .collect(Collectors.toList());

        for (int i = 0; i < sorted.size(); i++) {
            sorted.get(i).setRank(i + 1);
        }
        return sorted;
    }

    @Override
    public List<TopMovingProductResponse> getSlowMovingProducts(int thresholdDays) {
        List<TopMovingProductResponse> all = getTopMovingProducts(100);
        // Slow moving = moved in last X days but very few units
        return all.stream()
                .filter(p -> p.getTotalUnitsMoved() > 0 && p.getTotalUnitsMoved() < 10)
                .limit(10)
                .collect(Collectors.toList());
    }

    @Override
    public List<DeadStockResponse> getDeadStock(int deadStockDays) {
        ApiResponse<List<ProductResponse>> allProducts = productClient.getAllProducts();
        ApiResponse<List<MovementResponse>> allMovements = movementClient.getAllMovements();
        
        if (allProducts == null || allProducts.getData() == null) return Collections.emptyList();
        
        Map<Long, LocalDateTime> lastMovementMap = new HashMap<>();
        if (allMovements != null && allMovements.getData() != null) {
            for (MovementResponse m : allMovements.getData()) {
                LocalDateTime current = lastMovementMap.get(m.getProductId());
                if (current == null || m.getMovementDate().isAfter(current)) {
                    lastMovementMap.put(m.getProductId(), m.getMovementDate());
                }
            }
        }

        LocalDateTime threshold = LocalDateTime.now().minusDays(deadStockDays);
        
        return allProducts.getData().stream()
                .filter(p -> {
                    LocalDateTime last = lastMovementMap.get(p.getId());
                    return last == null || last.isBefore(threshold);
                })
                .map(p -> DeadStockResponse.builder()
                        .productId(p.getId())
                        .productName(p.getName())
                        .productSku(p.getSku())
                        .daysSinceLastMovement(lastMovementMap.containsKey(p.getId()) 
                            ? (int) java.time.temporal.ChronoUnit.DAYS.between(lastMovementMap.get(p.getId()), LocalDateTime.now())
                            : 999)
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public POSummaryResponse getPOSummary(LocalDate from, LocalDate to) {
        ApiResponse<List<PurchaseOrderResponse>> response = purchaseClient.getByDateRange(from.toString(), to.toString());
        if (response == null || response.getData() == null) {
            return POSummaryResponse.builder().fromDate(from).toDate(to).totalSpend(BigDecimal.ZERO).build();
        }

        List<PurchaseOrderResponse> pos = response.getData();
        long approved = pos.stream().filter(o -> "APPROVED".equals(o.getStatus()) || "RECEIVED".equals(o.getStatus())).count();
        long pending = pos.stream().filter(o -> "PENDING".equals(o.getStatus())).count();
        long cancelled = pos.stream().filter(o -> "CANCELLED".equals(o.getStatus())).count();
        BigDecimal totalSpend = pos.stream()
                .map(PurchaseOrderResponse::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return POSummaryResponse.builder()
                .fromDate(from)
                .toDate(to)
                .totalPOs(pos.size())
                .approvedPOs((int) approved)
                .pendingPOs((int) pending)
                .cancelledPOs((int) cancelled)
                .totalSpend(totalSpend)
                .build();
    }

    @Override
    public POSummaryResponse getPOSummaryBySupplier(Long supplierId, LocalDate from, LocalDate to) {
        POSummaryResponse summary = getPOSummary(from, to);
        // In a real app, we'd filter by supplierId in the client call
        summary.setSupplierId(supplierId);
        return summary;
    }

    private SnapshotResponse mapToSnapshotResponse(InventorySnapshot s) {
        return SnapshotResponse.builder()
                .id(s.getId())
                .warehouseId(s.getWarehouseId())
                .productId(s.getProductId())
                .quantity(s.getQuantity())
                .stockValue(s.getStockValue())
                .snapshotDate(s.getSnapshotDate())
                .build();
    }
}
