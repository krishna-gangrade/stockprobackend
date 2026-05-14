package com.stockpro.alert.controller;

import com.stockpro.alert.dto.AlertRequest;
import com.stockpro.alert.dto.AlertResponse;
import com.stockpro.alert.dto.BulkAlertRequest;
import com.stockpro.alert.entity.Alert.AlertType;
import com.stockpro.alert.entity.Alert.Severity;
import com.stockpro.alert.service.AlertService;
import com.stockpro.alert.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/alerts")
@RequiredArgsConstructor
@Tag(name = "Alerts", description = "Alert centre — send, read, acknowledge, and manage alerts")
public class AlertController {

    private final AlertService alertService;

    // ===== SEND =====

    @PostMapping
    @Operation(summary = "Send a single alert to a user")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Alert sent. CRITICAL alerts also dispatch email.")
    public ResponseEntity<ApiResponse<AlertResponse>> sendAlert(
            @Valid @RequestBody AlertRequest request) {
        AlertResponse response = alertService.sendAlert(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Alert sent", response));
    }

    @PostMapping("/bulk")
    @Operation(summary = "Send alert to multiple users at once")
    public ResponseEntity<ApiResponse<List<AlertResponse>>> sendBulkAlert(
            @Valid @RequestBody BulkAlertRequest request) {
        List<AlertResponse> responses = alertService.sendBulkAlert(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Bulk alert sent to " + responses.size() + " recipients",
                        responses));
    }

    // ===== READ — my alerts (current user) =====

    @GetMapping("/my")
    @Operation(summary = "Get all alerts for the current logged-in user")
    public ResponseEntity<ApiResponse<List<AlertResponse>>> getMyAlerts(
            @RequestParam Long userId) {
        return ResponseEntity.ok(
                ApiResponse.ok(alertService.getByRecipient(userId)));
    }

    @GetMapping("/my/unread")
    @Operation(summary = "Get unread alerts for the current user")
    public ResponseEntity<ApiResponse<List<AlertResponse>>> getMyUnread(
            @RequestParam Long userId) {
        return ResponseEntity.ok(
                ApiResponse.ok(alertService.getUnreadByRecipient(userId)));
    }

    @GetMapping("/my/unread/count")
    @Operation(summary = "Get unread alert count for the current user — used for badge in UI")
    public ResponseEntity<ApiResponse<Integer>> getUnreadCount(
            @RequestParam Long userId) {
        return ResponseEntity.ok(
                ApiResponse.ok(alertService.getUnreadCount(userId)));
    }

    @GetMapping("/my/unacknowledged")
    @Operation(summary = "Get unacknowledged alerts for the current user")
    public ResponseEntity<ApiResponse<List<AlertResponse>>> getMyUnacknowledged(
            @RequestParam Long userId) {
        return ResponseEntity.ok(
                ApiResponse.ok(alertService.getUnacknowledgedByRecipient(userId)));
    }

    // ===== READ — admin views =====

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all alerts in the system [ADMIN only]")
    public ResponseEntity<ApiResponse<List<AlertResponse>>> getAllAlerts() {
        return ResponseEntity.ok(ApiResponse.ok(alertService.getAllAlerts()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get alert by ID")
    public ResponseEntity<ApiResponse<AlertResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(alertService.getById(id)));
    }

    @GetMapping("/recipient/{recipientId}")
    @PreAuthorize("hasAnyRole('INVENTORY_MANAGER', 'ADMIN')")
    @Operation(summary = "Get alerts for a specific user [MANAGER, ADMIN]")
    public ResponseEntity<ApiResponse<List<AlertResponse>>> getByRecipient(
            @Parameter(description = "Recipient user ID") @PathVariable Long recipientId) {
        return ResponseEntity.ok(ApiResponse.ok(alertService.getByRecipient(recipientId)));
    }

    @GetMapping("/type/{type}")
    @PreAuthorize("hasAnyRole('INVENTORY_MANAGER', 'ADMIN')")
    @Operation(summary = "Get alerts by type (LOW_STOCK / OVERSTOCK / PO_PENDING etc.) [MANAGER, ADMIN]")
    public ResponseEntity<ApiResponse<List<AlertResponse>>> getByType(
            @PathVariable AlertType type) {
        return ResponseEntity.ok(ApiResponse.ok(alertService.getByType(type)));
    }

    @GetMapping("/severity/{severity}")
    @PreAuthorize("hasAnyRole('INVENTORY_MANAGER', 'ADMIN')")
    @Operation(summary = "Get alerts by severity (INFO / WARNING / CRITICAL) [MANAGER, ADMIN]")
    public ResponseEntity<ApiResponse<List<AlertResponse>>> getBySeverity(
            @PathVariable Severity severity) {
        return ResponseEntity.ok(ApiResponse.ok(alertService.getBySeverity(severity)));
    }

    // ===== STATE MANAGEMENT =====

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark a single alert as read")
    public ResponseEntity<ApiResponse<AlertResponse>> markAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.ok("Alert marked as read", alertService.markAsRead(id)));
    }

    @PutMapping("/my/read-all")
    @Operation(summary = "Mark ALL alerts as read for the current user")
    public ResponseEntity<ApiResponse<Void>> markAllRead(
            @RequestParam Long userId) {
        alertService.markAllReadForRecipient(userId);
        return ResponseEntity.ok(ApiResponse.ok("All alerts marked as read", null));
    }

    @PutMapping("/{id}/acknowledge")
    @Operation(summary = "Acknowledge (dismiss) an alert — also marks it as read")
    public ResponseEntity<ApiResponse<AlertResponse>> acknowledge(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.ok("Alert acknowledged", alertService.acknowledge(id)));
    }

    // ===== DELETE =====

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete an alert [ADMIN only]")
    public ResponseEntity<ApiResponse<Void>> deleteAlert(@PathVariable Long id) {
        alertService.deleteAlert(id);
        return ResponseEntity.ok(ApiResponse.ok("Alert deleted", null));
    }
}
