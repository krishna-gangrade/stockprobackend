package com.stockpro.purchase.repository;

import com.stockpro.purchase.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseRepository extends JpaRepository<PurchaseOrder, Long> {

    List<PurchaseOrder> findBySupplierId(Long supplierId);

    List<PurchaseOrder> findByWarehouseId(Long warehouseId);

    List<PurchaseOrder> findByStatus(PurchaseOrder.POStatus status);

    List<PurchaseOrder> findByCreatedById(Long createdById);

    List<PurchaseOrder> findByOrderDateBetween(LocalDate from, LocalDate to);

    Optional<PurchaseOrder> findByReferenceNumber(String referenceNumber);

    // POs that are approved but expected date has passed and not yet received
    // Used by the overdue PO alert scheduler
    List<PurchaseOrder> findByStatusAndExpectedDateBefore(
            PurchaseOrder.POStatus status, LocalDate date);

    boolean existsByReferenceNumber(String referenceNumber);
}
