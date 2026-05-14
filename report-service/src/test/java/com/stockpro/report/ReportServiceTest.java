package com.stockpro.report;

import com.stockpro.report.client.*;
import com.stockpro.report.dto.*;
import com.stockpro.report.repository.ReportRepository;
import com.stockpro.report.service.ReportServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock private ReportRepository reportRepository;
    @Mock private ProductClient    productClient;
    @Mock private WarehouseClient  warehouseClient;
    @Mock private MovementClient   movementClient;
    @Mock private PurchaseClient   purchaseClient;

    @InjectMocks
    private ReportServiceImpl reportService;

    @BeforeEach
    void setUp() {
    }

    // ===== takeSnapshot() =====

    @Test
    @DisplayName("takeSnapshot - basic success log")
    void takeSnapshot_success() {
        reportService.takeSnapshot();
        // Since we decoupled, it just logs and returns. No repository calls for now.
        verifyNoInteractions(reportRepository);
    }

    // ===== getTotalStockValue() =====

    @Test
    @DisplayName("getTotalStockValue - returns stubbed total")
    void getTotalStockValue_returnsStubbedTotal() {
        StockValuationResponse response = reportService.getTotalStockValue();
        assertThat(response.getTotalValue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ===== getInventoryTurnover() =====

    @Test
    @DisplayName("getInventoryTurnover - returns stubbed turnover")
    void getInventoryTurnover_returnsStubbedTurnover() {
        LocalDate from = LocalDate.now().minusMonths(1);
        LocalDate to   = LocalDate.now();

        TurnoverResponse response = reportService.getInventoryTurnover(from, to);
        assertThat(response.getTurnoverRate()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
