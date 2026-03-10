package com.lume.workspace.controller;

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
@RequestMapping({"/v1/billing", "/api/v1/billing"})
public class VersionedBillingController {

    private final WorkspaceCommercialService workspaceCommercialService;

    public VersionedBillingController(WorkspaceCommercialService workspaceCommercialService) {
        this.workspaceCommercialService = workspaceCommercialService;
    }

    @GetMapping("/subscription")
    public ResponseEntity<WorkspaceSubscriptionResponse> currentSubscription() {
        return ResponseEntity.ok(workspaceCommercialService.getCurrentSubscription());
    }

    @PatchMapping("/subscription")
    public ResponseEntity<WorkspaceSubscriptionResponse> updateSubscription(
            @Valid @RequestBody UpdateWorkspaceSubscriptionRequest request
    ) {
        return ResponseEntity.ok(workspaceCommercialService.updateCurrentSubscription(request));
    }

    @PostMapping("/credit-packs/purchase")
    public ResponseEntity<PurchaseCreditPackResponse> purchaseCreditPack(
            @Valid @RequestBody PurchaseCreditPackRequest request
    ) {
        return ResponseEntity.ok(workspaceCommercialService.purchaseCreditPack(request));
    }

    @PostMapping("/webhooks/provider-event")
    public ResponseEntity<BillingWebhookProcessResponse> processProviderWebhook(
            @Valid @RequestBody BillingWebhookEventRequest request,
            @RequestHeader("X-Lume-Billing-Signature") String signature
    ) {
        return ResponseEntity.ok(workspaceCommercialService.processProviderWebhook(request, signature));
    }

    @GetMapping("/invoices")
    public ResponseEntity<List<InvoiceResponse>> invoices() {
        return ResponseEntity.ok(workspaceCommercialService.listCurrentInvoices());
    }

    @GetMapping("/payment-events")
    public ResponseEntity<List<PaymentEventResponse>> paymentEvents() {
        return ResponseEntity.ok(workspaceCommercialService.listCurrentPaymentEvents());
    }
}
