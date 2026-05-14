package com.stockpro.report.scheduler;

import com.stockpro.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * SnapshotScheduler — takes a daily inventory snapshot at midnight.
 *
 * cron = "0 0 0 * * *"  →  second=0, minute=0, hour=0 (midnight), every day
 *
 * Each run saves one InventorySnapshot record per (warehouse, product) pair,
 * capturing the exact quantity and stock value at that moment.
 * This builds a historical dataset for trend analysis in analytics.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SnapshotScheduler {

    private final ReportService reportService;

    @Scheduled(cron = "0 0 0 * * *")
    public void runDailySnapshot() {
        log.info("SnapshotScheduler triggered — taking daily inventory snapshot");
        try {
            reportService.takeSnapshot();
        } catch (Exception e) {
            log.error("Daily snapshot failed: {}", e.getMessage());
        }
    }
}
