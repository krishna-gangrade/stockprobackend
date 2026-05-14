package com.stockpro.purchase.service;

import com.stockpro.purchase.exception.ApiException;
import com.stockpro.purchase.client.ProductClient;
import com.stockpro.purchase.client.WarehouseClient;
import com.stockpro.purchase.dto.*;
import com.stockpro.purchase.dto.external.ProductResponse;
import com.stockpro.purchase.dto.external.StockLevelResponse;
import com.stockpro.purchase.dto.external.StockUpdateRequest;
import com.stockpro.purchase.response.ApiResponse;
import com.stockpro.purchase.entity.POLineItem;
import com.stockpro.purchase.entity.PurchaseOrder;
import com.stockpro.purchase.entity.PurchaseOrder.POStatus;
import com.stockpro.purchase.kafka.POApprovalEvent;
import com.stockpro.purchase.kafka.POCreatedEvent;
import com.stockpro.purchase.kafka.POApprovalProducer;
import com.stockpro.purchase.repository.PurchaseRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchaseServiceImpl implements PurchaseService {

    private final PurchaseRepository   purchaseRepository;
    private final ProductClient        productClient;
    private final WarehouseClient       warehouseClient;
    private final POApprovalProducer   poApprovalProducer;

    // =====================================================================
    // CREATE PO
    // =====================================================================

    @Override
    @Transactional
    public PurchaseOrderResponse createPO(PurchaseOrderRequest request, Long createdById) {
        // Validate warehouse exists
        ApiResponse<Boolean> warehouseExists = warehouseClient.existsById(request.getWarehouseId());
        if (warehouseExists == null || !Boolean.TRUE.equals(warehouseExists.getData())) {
            throw new ApiException("Warehouse not found: " + request.getWarehouseId(),
                    HttpStatus.NOT_FOUND);
        }

        // Check reference number uniqueness if provided
        if (request.getReferenceNumber() != null && !request.getReferenceNumber().isBlank()
                && purchaseRepository.existsByReferenceNumber(request.getReferenceNumber())) {
            throw new ApiException("Reference number already exists: "
                    + request.getReferenceNumber(), HttpStatus.CONFLICT);
        }
        
        String refNum = (request.getReferenceNumber() == null || request.getReferenceNumber().isBlank()) 
                        ? null : request.getReferenceNumber();

        PurchaseOrder po = PurchaseOrder.builder()
                .supplierId(request.getSupplierId())
                .warehouseId(request.getWarehouseId())
                .createdById(createdById)
                .status(POStatus.DRAFT)
                .orderDate(request.getOrderDate())
                .expectedDate(request.getExpectedDate())
                .referenceNumber(refNum)
                .notes(request.getNotes())
                .createdAt(LocalDateTime.now())
                .lineItems(new ArrayList<>())
                .build();

        // Build line items and calculate total
        BigDecimal total = BigDecimal.ZERO;
        for (POLineItemRequest lineReq : request.getLineItems()) {
            findProductOrThrow(lineReq.getProductId());
            BigDecimal lineCost = lineReq.getUnitCost()
                    .multiply(BigDecimal.valueOf(lineReq.getQuantity()));

            POLineItem lineItem = POLineItem.builder()
                    .purchaseOrder(po)
                    .productId(lineReq.getProductId())
                    .quantity(lineReq.getQuantity())
                    .unitCost(lineReq.getUnitCost())
                    .totalCost(lineCost)
                    .receivedQty(0)
                    .build();

            po.getLineItems().add(lineItem);
            total = total.add(lineCost);
        }
        po.setTotalAmount(total);

        PurchaseOrder savedPo = purchaseRepository.save(po);
        PurchaseOrderResponse response = mapToResponse(savedPo);

        // Publish Created Event for Payment service
        try {
            poApprovalProducer.publishCreatedEvent(POCreatedEvent.builder()
                    .poId(response.getId())
                    .supplierId(response.getSupplierId())
                    .createdById(response.getCreatedById())
                    .totalAmount(response.getTotalAmount())
                    .dueDate(response.getExpectedDate())
                    .referenceNumber(response.getReferenceNumber())
                    .createdAt(response.getCreatedAt())
                    .build());
        } catch (Exception e) {
            // Log and continue
        }

        return response;
    }

    // =====================================================================
    // READ
    // =====================================================================

    @Override
    public PurchaseOrderResponse getById(Long id) {
        return mapToResponse(findPOOrThrow(id));
    }

    @Override
    public List<PurchaseOrderResponse> getAllPOs() {
        return purchaseRepository.findAll()
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<PurchaseOrderResponse> getBySupplier(Long supplierId) {
        return purchaseRepository.findBySupplierId(supplierId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<PurchaseOrderResponse> getByWarehouse(Long warehouseId) {
        return purchaseRepository.findByWarehouseId(warehouseId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<PurchaseOrderResponse> getByStatus(POStatus status) {
        return purchaseRepository.findByStatus(status)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<PurchaseOrderResponse> getByDateRange(LocalDate from, LocalDate to) {
        return purchaseRepository.findByOrderDateBetween(from, to)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<PurchaseOrderResponse> getOverduePOs() {
        // Approved POs whose expected delivery date has passed with no GRN
        return purchaseRepository
                .findByStatusAndExpectedDateBefore(POStatus.APPROVED, LocalDate.now())
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // =====================================================================
    // UPDATE PO (only DRAFT POs can be edited)
    // =====================================================================

    @Override
    @Transactional
    public PurchaseOrderResponse updatePO(Long id, PurchaseOrderRequest request) {
        PurchaseOrder po = findPOOrThrow(id);
        if (po.getStatus() != POStatus.DRAFT) {
            throw new ApiException(
                    "Only DRAFT purchase orders can be edited. Current status: " + po.getStatus());
        }
        po.setSupplierId(request.getSupplierId());
        po.setWarehouseId(request.getWarehouseId());
        po.setOrderDate(request.getOrderDate());
        po.setExpectedDate(request.getExpectedDate());
        po.setNotes(request.getNotes());

        // Rebuild line items
        po.getLineItems().clear();
        BigDecimal total = BigDecimal.ZERO;
        for (POLineItemRequest lineReq : request.getLineItems()) {
            findProductOrThrow(lineReq.getProductId());
            BigDecimal lineCost = lineReq.getUnitCost()
                    .multiply(BigDecimal.valueOf(lineReq.getQuantity()));
            POLineItem lineItem = POLineItem.builder()
                    .purchaseOrder(po)
                    .productId(lineReq.getProductId())
                    .quantity(lineReq.getQuantity())
                    .unitCost(lineReq.getUnitCost())
                    .totalCost(lineCost)
                    .receivedQty(0)
                    .build();
            po.getLineItems().add(lineItem);
            total = total.add(lineCost);
        }
        po.setTotalAmount(total);
        return mapToResponse(purchaseRepository.save(po));
    }

    // =====================================================================
    // PO STATUS TRANSITIONS
    // =====================================================================

    @Override
    @Transactional
    public void submitForApproval(Long id) {
        PurchaseOrder po = findPOOrThrow(id);
        if (po.getStatus() != POStatus.DRAFT) {
            throw new ApiException("Only DRAFT POs can be submitted for approval");
        }
        po.setStatus(POStatus.PENDING);
        purchaseRepository.save(po);
    }

    @Override
    @Transactional
    public void approvePO(Long id, Long approvedById) {
        PurchaseOrder po = findPOOrThrow(id);
        if (po.getStatus() != POStatus.PENDING) {
            throw new ApiException("Only PENDING POs can be approved");
        }
        po.setStatus(POStatus.APPROVED);
        purchaseRepository.save(po);

        // ---- Publish Kafka event ----
        // This notifies the Alert service to send a notification
        // The PO approval succeeds regardless of whether Kafka is available
        POApprovalEvent event = POApprovalEvent.builder()
                .poId(po.getId())
                .referenceNumber(po.getReferenceNumber())
                .supplierId(po.getSupplierId())
                .warehouseId(po.getWarehouseId())
                .approvedById(approvedById)
                .totalAmount(po.getTotalAmount())
                .status("APPROVED")
                .eventTime(LocalDateTime.now())
                .build();
        poApprovalProducer.publishApprovalEvent(event);
    }

    @Override
    @Transactional
    public void rejectPO(Long id, Long rejectedById, String reason) {
        PurchaseOrder po = findPOOrThrow(id);
        if (po.getStatus() != POStatus.PENDING) {
            throw new ApiException("Only PENDING POs can be rejected");
        }
        po.setStatus(POStatus.DRAFT); // rejected POs go back to DRAFT for editing
        purchaseRepository.save(po);

        // Publish rejection event so purchase officer gets notified
        POApprovalEvent event = POApprovalEvent.builder()
                .poId(po.getId())
                .referenceNumber(po.getReferenceNumber())
                .supplierId(po.getSupplierId())
                .warehouseId(po.getWarehouseId())
                .approvedById(rejectedById)
                .totalAmount(po.getTotalAmount())
                .status("REJECTED")
                .reason(reason)
                .eventTime(LocalDateTime.now())
                .build();
        poApprovalProducer.publishApprovalEvent(event);
    }

    @Override
    @Transactional
    public void cancelPO(Long id, String reason) {
        PurchaseOrder po = findPOOrThrow(id);
        if (po.getStatus() == POStatus.RECEIVED || po.getStatus() == POStatus.CANCELLED) {
            throw new ApiException("Cannot cancel a PO that is already "
                    + po.getStatus().name().toLowerCase());
        }
        po.setStatus(POStatus.CANCELLED);
        po.setNotes((po.getNotes() != null ? po.getNotes() + " | " : "") + "Cancelled: " + reason);
        purchaseRepository.save(po);
    }

    // =====================================================================
    // GOODS RECEIPT — supports partial receipt per line item
    // =====================================================================

    @Override
    @Transactional
    public PurchaseOrderResponse receiveGoods(Long poId,
                                               List<ReceiveGoodsRequest> receipts) {
        PurchaseOrder po = findPOOrThrow(poId);

        if (po.getStatus() != POStatus.APPROVED
                && po.getStatus() != POStatus.PARTIALLY_RECEIVED) {
            throw new ApiException(
                    "Goods can only be received against APPROVED or PARTIALLY_RECEIVED POs. "
                    + "Current status: " + po.getStatus());
        }

        for (ReceiveGoodsRequest receipt : receipts) {
            // Find the matching line item in this PO
            POLineItem lineItem = po.getLineItems().stream()
                    .filter(li -> li.getId().equals(receipt.getLineItemId()))
                    .findFirst()
                    .orElseThrow(() -> new ApiException(
                            "Line item " + receipt.getLineItemId()
                            + " not found in PO " + poId));

            int pendingQty = lineItem.getPendingQty();
            if (receipt.getReceivedQuantity() > pendingQty) {
                throw new ApiException(
                        "Cannot receive " + receipt.getReceivedQuantity()
                        + " units for line item " + lineItem.getId()
                        + ". Pending: " + pendingQty);
            }

            // Update received quantity on the line item
            lineItem.setReceivedQty(lineItem.getReceivedQty() + receipt.getReceivedQuantity());

            // Update stock level in warehouse — add received qty to on-hand quantity
            updateStockOnReceipt(po.getWarehouseId(), lineItem.getProductId(),
                    receipt.getReceivedQuantity());
        }

        // Determine new PO status based on whether all lines are fully received
        boolean allReceived = po.getLineItems().stream()
                .allMatch(POLineItem::isFullyReceived);

        if (allReceived) {
            po.setStatus(POStatus.RECEIVED);
            po.setReceivedDate(LocalDate.now());
        } else {
            po.setStatus(POStatus.PARTIALLY_RECEIVED);
        }

        return mapToResponse(purchaseRepository.save(po));
    }

    // =====================================================================
    // PRIVATE HELPERS
    // =====================================================================

    private void updateStockOnReceipt(Long warehouseId, Long productId, int receivedQty) {
        // Fetch current stock to calculate new total
        int currentQty = getCurrentStockQuantity(warehouseId, productId);

        StockUpdateRequest updateRequest = StockUpdateRequest.builder()
                .warehouseId(warehouseId)
                .productId(productId)
                .quantity(currentQty + receivedQty)
                .build();

        warehouseClient.updateStock(updateRequest);
    }

    private int getCurrentStockQuantity(Long warehouseId, Long productId) {
        try {
            ApiResponse<StockLevelResponse> currentStock = warehouseClient.getStockLevel(warehouseId, productId);
            if (currentStock != null && currentStock.isSuccess() && currentStock.getData() != null) {
                return currentStock.getData().getQuantity();
            }
        } catch (FeignException ex) {
            if (ex.status() == HttpStatus.NOT_FOUND.value()
                    || ex.status() == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                return 0;
            }
            throw ex;
        }
        return 0;
    }

    @SuppressWarnings("null")
    private PurchaseOrder findPOOrThrow(Long id) {
        return purchaseRepository.findById(id)
                .orElseThrow(() -> new ApiException(
                        "Purchase Order not found with id: " + id, HttpStatus.NOT_FOUND));
    }

    private ProductResponse findProductOrThrow(Long id) {
        ApiResponse<ProductResponse> response = productClient.getById(id);
        if (response == null || !response.isSuccess() || response.getData() == null) {
            throw new ApiException("Product not found with id: " + id, HttpStatus.NOT_FOUND);
        }
        return response.getData();
    }

    private PurchaseOrderResponse mapToResponse(PurchaseOrder po) {
        List<POLineItemResponse> lineResponses = po.getLineItems().stream()
                .map(li -> {
                    ProductResponse product = null;
                    try {
                        product = findProductOrThrow(li.getProductId());
                    } catch (Exception e) {}
                    
                    String productName = (product != null) ? product.getName() : "Unknown";
                    String productSku = (product != null) ? product.getSku() : "N/A";
                    return POLineItemResponse.builder()
                            .id(li.getId())
                            .productId(li.getProductId())
                            .productName(productName)
                            .productSku(productSku)
                            .quantity(li.getQuantity())
                            .unitCost(li.getUnitCost())
                            .totalCost(li.getTotalCost())
                            .receivedQty(li.getReceivedQty())
                            .pendingQty(li.getPendingQty())
                            .fullyReceived(li.isFullyReceived())
                            .build();
                })
                .collect(Collectors.toList());

        return PurchaseOrderResponse.builder()
                .id(po.getId())
                .supplierId(po.getSupplierId())
                .warehouseId(po.getWarehouseId())
                .createdById(po.getCreatedById())
                .status(po.getStatus())
                .totalAmount(po.getTotalAmount())
                .orderDate(po.getOrderDate())
                .expectedDate(po.getExpectedDate())
                .receivedDate(po.getReceivedDate())
                .referenceNumber(po.getReferenceNumber())
                .notes(po.getNotes())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .lineItems(lineResponses)
                .build();
    }
}
