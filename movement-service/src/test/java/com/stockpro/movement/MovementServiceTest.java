package com.stockpro.movement;

import com.stockpro.movement.dto.MovementRequest;
import com.stockpro.movement.dto.MovementResponse;
import com.stockpro.movement.client.ProductClient;
import com.stockpro.movement.client.WarehouseClient;
import com.stockpro.movement.entity.StockMovement;
import com.stockpro.movement.entity.StockMovement.MovementType;
import com.stockpro.movement.kafka.MovementProducer;
import com.stockpro.movement.repository.MovementRepository;
import com.stockpro.movement.response.ApiResponse;
import com.stockpro.movement.dto.external.ProductResponse;
import com.stockpro.movement.dto.external.StockLevelResponse;
import com.stockpro.movement.dto.external.WarehouseResponse;
import com.stockpro.movement.service.MovementServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
public class MovementServiceTest {

    @Mock private MovementRepository movementRepository;
    @Mock private ProductClient productClient;
    @Mock private WarehouseClient warehouseClient;
    @Mock private MovementProducer movementProducer;

    @InjectMocks
    private MovementServiceImpl movementService;

    @BeforeEach
    void setUp() {
    }

    // ===== recordMovement() =====

    @Test
    @DisplayName("recordMovement - records movement correctly")
    void recordMovement_success() {
        MovementRequest request = MovementRequest.builder()
                .productId(10L).warehouseId(1L)
                .movementType(MovementType.STOCK_IN)
                .quantity(50)
                .notes("GRN from PO-001")
                .build();

        StockMovement savedMovement = StockMovement.builder()
                .id(1L)
                .productId(10L)
                .warehouseId(1L)
                .fromWarehouseId(1L)
                .toWarehouseId(1L)
                .movementType(MovementType.STOCK_IN)
                .quantity(50)
                .balanceAfter(50)
                .movementDate(LocalDateTime.now())
                .build();

        ProductResponse product = new ProductResponse();
        product.setId(10L);
        product.setName("Phone");
        product.setSku("PHN-01");
        WarehouseResponse warehouse = new WarehouseResponse();
        warehouse.setId(1L);
        warehouse.setName("Main Warehouse");

        when(productClient.getById(10L)).thenReturn(ApiResponse.ok(product));
        when(warehouseClient.getById(1L)).thenReturn(ApiResponse.ok(warehouse));
        when(warehouseClient.getStockLevel(1L, 10L)).thenThrow(feign.FeignException.NotFound.class);
        when(movementRepository.save(any(StockMovement.class))).thenReturn(savedMovement);
        when(warehouseClient.updateStock(any())).thenReturn(ApiResponse.ok(new StockLevelResponse()));

        MovementResponse response = movementService.recordMovement(request, 2L);

        assertThat(response.getMovementType()).isEqualTo(MovementType.STOCK_IN);
        assertThat(response.getQuantity()).isEqualTo(50);
        verify(movementRepository).save(any(StockMovement.class));
    }

    @Test
    @DisplayName("recordMovement - supplier return reduces stock")
    void recordMovement_supplierReturnReducesStock() {
        MovementRequest request = MovementRequest.builder()
                .productId(10L)
                .warehouseId(1L)
                .movementType(MovementType.RETURN)
                .referenceType("SUPPLIER_RETURN")
                .quantity(5)
                .notes("Returned damaged stock to supplier")
                .build();

        StockMovement savedMovement = StockMovement.builder()
                .id(2L)
                .productId(10L)
                .warehouseId(1L)
                .fromWarehouseId(1L)
                .toWarehouseId(1L)
                .movementType(MovementType.RETURN)
                .referenceType("SUPPLIER_RETURN")
                .quantity(5)
                .balanceAfter(15)
                .performedBy(2L)
                .movementDate(LocalDateTime.now())
                .build();

        ProductResponse product = new ProductResponse();
        product.setId(10L);
        product.setName("Phone");
        product.setSku("PHN-01");
        WarehouseResponse warehouse = new WarehouseResponse();
        warehouse.setId(1L);
        warehouse.setName("Main Warehouse");

        StockLevelResponse stockLevel = new StockLevelResponse();
        stockLevel.setQuantity(20);

        when(productClient.getById(10L)).thenReturn(ApiResponse.ok(product));
        when(warehouseClient.getById(1L)).thenReturn(ApiResponse.ok(warehouse));
        when(warehouseClient.getStockLevel(1L, 10L)).thenReturn(ApiResponse.ok(stockLevel));
        when(movementRepository.save(any(StockMovement.class))).thenReturn(savedMovement);
        when(warehouseClient.updateStock(any())).thenReturn(ApiResponse.ok(new StockLevelResponse()));

        MovementResponse response = movementService.recordMovement(request, 2L);

        assertThat(response.getMovementType()).isEqualTo(MovementType.RETURN);
        assertThat(response.getBalanceAfter()).isEqualTo(15);
        verify(movementRepository).save(any(StockMovement.class));
    }

    // ===== getByProduct() =====

    @Test
    @DisplayName("getByProduct - returns movements")
    void getByProduct_returnsMovements() {
        StockMovement movement = StockMovement.builder()
                .id(1L).productId(10L).warehouseId(1L)
                .fromWarehouseId(1L).toWarehouseId(1L)
                .movementType(MovementType.STOCK_IN)
                .quantity(50).balanceAfter(50).performedBy(2L)
                .movementDate(LocalDateTime.now()).build();

        ProductResponse product = new ProductResponse();
        product.setId(10L);
        product.setName("Phone");
        product.setSku("PHN-01");
        WarehouseResponse warehouse = new WarehouseResponse();
        warehouse.setId(1L);
        warehouse.setName("Main Warehouse");

        when(movementRepository.findByProductIdOrderByMovementDateDesc(10L))
                .thenReturn(List.of(movement));
        when(productClient.getById(10L)).thenReturn(ApiResponse.ok(product));
        when(warehouseClient.getById(1L)).thenReturn(ApiResponse.ok(warehouse));

        List<MovementResponse> results = movementService.getByProduct(10L);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getMovementType()).isEqualTo(MovementType.STOCK_IN);
    }

    // ===== getTotalStockIn / getTotalStockOut =====

    @Test
    @DisplayName("getTotalStockIn - returns correct total")
    void getTotalStockIn_returnsCorrectTotal() {
        when(movementRepository.sumStockIn(10L, 1L)).thenReturn(350);

        int total = movementService.getTotalStockIn(10L, 1L);

        assertThat(total).isEqualTo(350);
    }

    @Test
    @DisplayName("getTotalStockOut - returns correct total")
    void getTotalStockOut_returnsCorrectTotal() {
        when(movementRepository.sumStockOut(10L, 1L)).thenReturn(120);

        int total = movementService.getTotalStockOut(10L, 1L);

        assertThat(total).isEqualTo(120);
    }
}
