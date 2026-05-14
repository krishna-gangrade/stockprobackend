package com.stockpro.warehouse;

import com.stockpro.warehouse.client.ProductClient;
import com.stockpro.warehouse.dto.StockLevelResponse;
import com.stockpro.warehouse.dto.StockUpdateRequest;
import com.stockpro.warehouse.dto.TransferRequest;
import com.stockpro.warehouse.dto.external.ProductResponse;
import com.stockpro.warehouse.entity.StockLevel;
import com.stockpro.warehouse.entity.Warehouse;
import com.stockpro.warehouse.exception.ApiException;
import com.stockpro.warehouse.repository.StockLevelRepository;
import com.stockpro.warehouse.repository.WarehouseRepository;
import com.stockpro.warehouse.response.ApiResponse;
import com.stockpro.warehouse.service.WarehouseServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class WarehouseServiceImplTest {

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private StockLevelRepository stockLevelRepository;

    @Mock
    private ProductClient productClient;

    @InjectMocks
    private WarehouseServiceImpl warehouseService;

    @Test
    @DisplayName("updateStock blocks additions that exceed warehouse capacity")
    void updateStock_blocksOverCapacity() {
        Warehouse warehouse = warehouse(1L, 10, 8);
        StockLevel existing = stockLevel(1L, 1L, 100L, 3);

        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(productClient.getById(100L)).thenReturn(ApiResponse.ok(product(100L)));
        when(stockLevelRepository.findByWarehouseIdAndProductId(1L, 100L)).thenReturn(Optional.of(existing));
        when(stockLevelRepository.findByWarehouseId(1L)).thenReturn(List.of(
                stockLevel(2L, 1L, 101L, 5),
                existing
        ));

        StockUpdateRequest request = StockUpdateRequest.builder()
                .warehouseId(1L)
                .productId(100L)
                .quantity(6)
                .build();

        assertThatThrownBy(() -> warehouseService.updateStock(request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Not enough warehouse capacity");

        verify(stockLevelRepository, never()).save(any(StockLevel.class));
    }

    @Test
    @DisplayName("updateStock allows additions that fit in warehouse capacity")
    void updateStock_allowsWithinCapacity() {
        Warehouse warehouse = warehouse(1L, 10, 8);
        StockLevel existing = stockLevel(1L, 1L, 100L, 3);

        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(productClient.getById(100L)).thenReturn(ApiResponse.ok(product(100L)));
        when(stockLevelRepository.findByWarehouseIdAndProductId(1L, 100L)).thenReturn(Optional.of(existing));
        when(stockLevelRepository.findByWarehouseId(1L)).thenReturn(List.of(
                stockLevel(2L, 1L, 101L, 5),
                existing
        ));
        when(stockLevelRepository.save(any(StockLevel.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StockUpdateRequest request = StockUpdateRequest.builder()
                .warehouseId(1L)
                .productId(100L)
                .quantity(5)
                .build();

        StockLevelResponse response = warehouseService.updateStock(request);

        assertThat(response.getQuantity()).isEqualTo(5);
        verify(stockLevelRepository).save(any(StockLevel.class));
    }

    @Test
    @DisplayName("transferStock blocks destination warehouse overflow")
    void transferStock_blocksDestinationOverflow() {
        Warehouse sourceWarehouse = warehouse(1L, 20, 10);
        Warehouse destinationWarehouse = warehouse(2L, 10, 9);
        StockLevel source = stockLevel(1L, 1L, 100L, 4);
        StockLevel destination = stockLevel(2L, 2L, 100L, 4);

        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(sourceWarehouse));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(destinationWarehouse));
        when(productClient.getById(100L)).thenReturn(ApiResponse.ok(product(100L)));
        when(stockLevelRepository.findByWarehouseIdAndProductId(1L, 100L)).thenReturn(Optional.of(source));
        when(stockLevelRepository.findByWarehouseIdAndProductId(2L, 100L)).thenReturn(Optional.of(destination));
        when(stockLevelRepository.findByWarehouseId(2L)).thenReturn(List.of(
                stockLevel(3L, 2L, 101L, 5),
                destination
        ));

        TransferRequest request = TransferRequest.builder()
                .fromWarehouseId(1L)
                .toWarehouseId(2L)
                .productId(100L)
                .quantity(2)
                .build();

        assertThatThrownBy(() -> warehouseService.transferStock(request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Not enough warehouse capacity");

        verify(stockLevelRepository, never()).save(any(StockLevel.class));
    }

    private Warehouse warehouse(Long id, int capacity, int usedCapacity) {
        return Warehouse.builder()
                .id(id)
                .name("Warehouse-" + id)
                .location("Loc")
                .capacity(capacity)
                .usedCapacity(usedCapacity)
                .isActive(true)
                .build();
    }

    private StockLevel stockLevel(Long id, Long warehouseId, Long productId, int quantity) {
        return StockLevel.builder()
                .id(id)
                .warehouseId(warehouseId)
                .productId(productId)
                .quantity(quantity)
                .reservedQuantity(0)
                .build();
    }

    private ProductResponse product(Long id) {
        ProductResponse product = new ProductResponse();
        product.setId(id);
        product.setName("Product-" + id);
        product.setSku("SKU-" + id);
        product.setActive(true);
        return product;
    }
}
