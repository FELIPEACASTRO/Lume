package com.lume.workspace.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.lume.workspace.dto.BillingWebhookEventRequest;
import com.lume.workspace.dto.BillingWebhookProcessResponse;
import com.lume.workspace.dto.InvoiceResponse;
import com.lume.workspace.dto.PaymentEventResponse;
import com.lume.workspace.dto.PurchaseCreditPackRequest;
import com.lume.workspace.dto.PurchaseCreditPackResponse;
import com.lume.workspace.dto.UpdateWorkspaceSubscriptionRequest;
import com.lume.workspace.dto.WorkspaceSubscriptionResponse;
import com.lume.workspace.service.WorkspaceCommercialService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/billing")
@Tag(name = "Billing", description = "Subscription management, credit purchases, invoices, and payment webhooks")
public class VersionedBillingController {

    private final WorkspaceCommercialService workspaceCommercialService;

    public VersionedBillingController(WorkspaceCommercialService workspaceCommercialService) {
        this.workspaceCommercialService = workspaceCommercialService;
    }

    @Operation(summary = "Get current subscription", description = "Returns the active subscription for the current workspace.")
    @ApiResponse(responseCode = "200", description = "Subscription retrieved successfully")
    @GetMapping("/subscription")
    public ResponseEntity<WorkspaceSubscriptionResponse> currentSubscription() {
        return ResponseEntity.ok(workspaceCommercialService.getCurrentSubscription());
    }

    @Operation(summary = "Update subscription", description = "Patches the current workspace subscription with the provided fields.")
    @ApiResponse(responseCode = "200", description = "Subscription updated successfully")
    @PatchMapping("/subscription")
    public ResponseEntity<WorkspaceSubscriptionResponse> updateSubscription(
            @Valid @RequestBody UpdateWorkspaceSubscriptionRequest request
    ) {
        return ResponseEntity.ok(workspaceCommercialService.updateCurrentSubscription(request));
    }

    @Operation(summary = "Purchase credit pack", description = "Initiates a credit pack purchase and returns a checkout URL.")
    @ApiResponse(responseCode = "200", description = "Credit pack purchase initiated successfully")
    @PostMapping("/credit-packs/purchase")
    public ResponseEntity<PurchaseCreditPackResponse> purchaseCreditPack(
            @Valid @RequestBody PurchaseCreditPackRequest request
    ) {
        return ResponseEntity.ok(workspaceCommercialService.purchaseCreditPack(request));
    }

    @Operation(summary = "Process billing provider webhook", description = "Verifies signature and processes a billing event from the payment provider.")
    @ApiResponse(responseCode = "200", description = "Webhook processed successfully")
    @PostMapping("/webhooks/provider-event")
    public ResponseEntity<BillingWebhookProcessResponse> processProviderWebhook(
            @Valid @RequestBody BillingWebhookEventRequest request,
            @RequestHeader("X-Lume-Billing-Signature") String signature
    ) {
        return ResponseEntity.ok(workspaceCommercialService.processProviderWebhook(request, signature));
    }

    @Operation(summary = "Process MercadoPago webhook", description = "Verifies signature and processes a billing event from MercadoPago.")
    @ApiResponse(responseCode = "200", description = "Webhook processed successfully")
    @PostMapping("/webhooks/mercado-pago")
    public ResponseEntity<BillingWebhookProcessResponse> processMercadoPagoWebhook(
            @Valid @RequestBody BillingWebhookEventRequest request,
            @RequestHeader("X-MercadoPago-Signature") String signature
    ) {
        return ResponseEntity.ok(workspaceCommercialService.processProviderWebhook(request, signature));
    }

    @Operation(summary = "List invoices", description = "Returns all invoices for the current workspace.")
    @ApiResponse(responseCode = "200", description = "Invoices retrieved successfully")
    @GetMapping("/invoices")
    public ResponseEntity<List<InvoiceResponse>> invoices() {
        return ResponseEntity.ok(workspaceCommercialService.listCurrentInvoices());
    }

    @Operation(summary = "List payment events", description = "Returns all payment events for the current workspace.")
    @ApiResponse(responseCode = "200", description = "Payment events retrieved successfully")
    @GetMapping("/payment-events")
    public ResponseEntity<List<PaymentEventResponse>> paymentEvents() {
        return ResponseEntity.ok(workspaceCommercialService.listCurrentPaymentEvents());
    }
}
