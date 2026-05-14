package com.stockpro.purchase.service;

import com.stockpro.purchase.dto.*;
import com.stockpro.purchase.entity.PurchaseOrder;

import java.time.LocalDate;
import java.util.List;

public interface PurchaseService {

    // ===== PO Lifecycle =====
    PurchaseOrderResponse  createPO(PurchaseOrderRequest request, Long createdById);
    PurchaseOrderResponse  getById(Long id);
    PurchaseOrderResponse  updatePO(Long id, PurchaseOrderRequest request);
    void                   submitForApproval(Long id);
    void                   approvePO(Long id, Long approvedById);
    void                   rejectPO(Long id, Long rejectedById, String reason);
    void                   cancelPO(Long id, String reason);

    // ===== Goods Receipt =====
    PurchaseOrderResponse  receiveGoods(Long poId, List<ReceiveGoodsRequest> receipts);

    // ===== Queries =====
    List<PurchaseOrderResponse> getAllPOs();
    List<PurchaseOrderResponse> getBySupplier(Long supplierId);
    List<PurchaseOrderResponse> getByWarehouse(Long warehouseId);
    List<PurchaseOrderResponse> getByStatus(PurchaseOrder.POStatus status);
    List<PurchaseOrderResponse> getByDateRange(LocalDate from, LocalDate to);
    List<PurchaseOrderResponse> getOverduePOs();
}
