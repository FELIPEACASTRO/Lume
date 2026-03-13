package com.lume.workspace.service;

import com.lume.workspace.dto.BillingWebhookEventRequest;
import com.lume.workspace.dto.BillingWebhookProcessResponse;
import com.lume.workspace.dto.BootstrapSetupRequest;
import com.lume.workspace.dto.InvoiceResponse;
import com.lume.workspace.dto.PaymentEventResponse;
import com.lume.workspace.dto.PurchaseCreditPackRequest;
import com.lume.workspace.dto.PurchaseCreditPackResponse;
import com.lume.workspace.dto.UpdateWorkspaceOnboardingRequest;
import com.lume.workspace.dto.UpdateWorkspaceSubscriptionRequest;
import com.lume.workspace.dto.WorkspaceCommercialSummaryResponse;
import com.lume.workspace.dto.WorkspaceOnboardingResponse;
import com.lume.workspace.dto.WorkspaceSubscriptionResponse;
import com.lume.workspace.entity.WorkspaceInvoiceJpaEntity;
import com.lume.workspace.entity.WorkspaceOnboardingProfileJpaEntity;
import com.lume.workspace.entity.WorkspacePaymentEventJpaEntity;
import com.lume.workspace.entity.WorkspaceSubscriptionJpaEntity;
import com.lume.workspace.repository.WorkspaceInvoiceJpaRepository;
import com.lume.workspace.repository.WorkspacePaymentEventJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class WorkspaceCommercialService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final WorkspaceOnboardingService workspaceOnboardingService;
    private final WorkspaceSubscriptionService workspaceSubscriptionService;
    private final BillingWebhookService billingWebhookService;
    private final WorkspaceInvoiceJpaRepository invoiceRepository;
    private final WorkspacePaymentEventJpaRepository paymentEventRepository;
    private final WorkspaceContextService workspaceContextService;
    private final AuditLogService auditLogService;
    private final WorkspaceLedgerService workspaceLedgerService;
    private final MercadoPagoCheckoutAdapter mercadoPagoCheckoutAdapter;

    public WorkspaceCommercialService(
            WorkspaceOnboardingService workspaceOnboardingService,
            WorkspaceSubscriptionService workspaceSubscriptionService,
            BillingWebhookService billingWebhookService,
            WorkspaceInvoiceJpaRepository invoiceRepository,
            WorkspacePaymentEventJpaRepository paymentEventRepository,
            WorkspaceContextService workspaceContextService,
            AuditLogService auditLogService,
            WorkspaceLedgerService workspaceLedgerService,
            MercadoPagoCheckoutAdapter mercadoPagoCheckoutAdapter
    ) {
        this.workspaceOnboardingService = workspaceOnboardingService;
        this.workspaceSubscriptionService = workspaceSubscriptionService;
        this.billingWebhookService = billingWebhookService;
        this.invoiceRepository = invoiceRepository;
        this.paymentEventRepository = paymentEventRepository;
        this.workspaceContextService = workspaceContextService;
        this.auditLogService = auditLogService;
        this.workspaceLedgerService = workspaceLedgerService;
        this.mercadoPagoCheckoutAdapter = mercadoPagoCheckoutAdapter;
    }

    // ── summary (used by SettingsService, UsageService) ──────────────────

    public WorkspaceCommercialSummaryResponse getCurrentSummary() {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_WORKSPACE_READ);
        return getSummaryForWorkspace(workspaceContextService.getWorkspaceId());
    }

    public WorkspaceCommercialSummaryResponse getSummaryForWorkspace(Long workspaceId) {
        WorkspaceOnboardingProfileJpaEntity onboarding = workspaceOnboardingService.getOrCreateOnboarding(workspaceId);
        WorkspaceSubscriptionJpaEntity subscription = workspaceSubscriptionService.getOrCreateSubscription(workspaceId);
        return toSummary(subscription, onboarding);
    }

    // ── invoice & payment event listing ──────────────────────────────────

    public java.util.List<InvoiceResponse> listCurrentInvoices() {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_WORKSPACE_READ);
        return invoiceRepository.findByWorkspaceIdOrderByCreatedAtDesc(
                workspaceContextService.getWorkspaceId(), org.springframework.data.domain.PageRequest.of(0, 200))
                .stream()
                .map(this::toInvoiceResponse)
                .toList();
    }

    public java.util.List<PaymentEventResponse> listCurrentPaymentEvents() {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_WORKSPACE_READ);
        return paymentEventRepository.findByWorkspaceIdOrderByOccurredAtDesc(
                workspaceContextService.getWorkspaceId(), org.springframework.data.domain.PageRequest.of(0, 200))
                .stream()
                .map(this::toPaymentEventResponse)
                .toList();
    }

    // ── credit pack purchase ─────────────────────────────────────────────

    @Transactional
    public PurchaseCreditPackResponse purchaseCreditPack(PurchaseCreditPackRequest request) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_SETTINGS_MANAGE);
        Long workspaceId = workspaceContextService.getWorkspaceId();

        WorkspaceSubscriptionJpaEntity subscription = workspaceSubscriptionService.getOrCreateSubscription(workspaceId);
        workspaceSubscriptionService.validateSubscription(subscription);

        WorkspaceInvoiceJpaEntity invoice = new WorkspaceInvoiceJpaEntity();
        invoice.setWorkspaceId(workspaceId);
        invoice.setInvoiceNumber("INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT));
        invoice.setStatus("pending");
        invoice.setAmountBrl(request.amountBrl().setScale(2, java.math.RoundingMode.HALF_UP));
        invoice.setCurrency("BRL");
        invoice.setDueAt(LocalDate.now().plusDays(1));
        invoice.setPaidAt(null);
        invoice.setDescription(request.description() == null || request.description().isBlank()
                ? "Compra de pack de creditos: " + request.packCode()
                : request.description().trim());
        WorkspaceInvoiceJpaEntity savedInvoice = invoiceRepository.save(invoice);

        MercadoPagoCheckoutAdapter.MercadoPagoCheckoutSession checkoutSession = mercadoPagoCheckoutAdapter.createCreditPackCheckout(
                workspaceId,
                savedInvoice.getInvoiceNumber(),
                request.packCode(),
                request.credits(),
                savedInvoice.getAmountBrl(),
                savedInvoice.getDescription()
        );

        WorkspacePaymentEventJpaEntity paymentEvent = new WorkspacePaymentEventJpaEntity();
        paymentEvent.setWorkspaceId(workspaceId);
        paymentEvent.setInvoice(savedInvoice);
        paymentEvent.setGatewayEventId(checkoutSession.preferenceId());
        paymentEvent.setEventType("checkout_created");
        paymentEvent.setStatus("pending");
        paymentEvent.setAmountBrl(savedInvoice.getAmountBrl());
        paymentEvent.setPayloadHash(billingWebhookService.sha256Hex(checkoutSession.externalReference()));
        paymentEvent.setOccurredAt(java.time.LocalDateTime.now());
        paymentEvent.setProcessedAt(java.time.LocalDateTime.now());
        WorkspacePaymentEventJpaEntity savedPayment = paymentEventRepository.save(paymentEvent);

        workspaceLedgerService.recordUsageEvent(
                "billing.credit_pack_checkout_created",
                "workspace_subscription",
                String.valueOf(workspaceId),
                "Checkout Mercado Pago iniciado para pack " + request.packCode() + "."
        );

        auditLogService.record(
                "workspace_billing",
                String.valueOf(workspaceId),
                "credit_pack_checkout_created",
                Map.of(
                        "invoiceNumber", savedInvoice.getInvoiceNumber(),
                        "packCode", request.packCode(),
                        "credits", request.credits(),
                        "checkoutPreferenceId", checkoutSession.preferenceId()
                )
        );

        return new PurchaseCreditPackResponse(
                workspaceSubscriptionService.toSubscriptionResponse(subscription),
                toInvoiceResponse(savedInvoice),
                toPaymentEventResponse(savedPayment),
                checkoutSession.checkoutUrl(),
                checkoutSession.paymentStatus(),
                checkoutSession.externalReference()
        );
    }

    // ── delegation to split services ─────────────────────────────────────

    @Transactional
    public void initializeWorkspace(Long workspaceId, BootstrapSetupRequest request) {
        workspaceOnboardingService.initializeWorkspace(workspaceId, request);
    }

    public WorkspaceOnboardingResponse getCurrentOnboarding() {
        return workspaceOnboardingService.getCurrentOnboarding();
    }

    @Transactional
    public WorkspaceOnboardingResponse updateCurrentOnboarding(UpdateWorkspaceOnboardingRequest request) {
        return workspaceOnboardingService.updateCurrentOnboarding(request);
    }

    @Transactional
    public WorkspaceOnboardingResponse advanceCurrentOnboarding(String activationStatus, String activationNote, String sourceEvent) {
        return workspaceOnboardingService.advanceCurrentOnboarding(activationStatus, activationNote, sourceEvent);
    }

    public WorkspaceSubscriptionResponse getCurrentSubscription() {
        return workspaceSubscriptionService.getCurrentSubscription();
    }

    @Transactional
    public WorkspaceSubscriptionResponse updateCurrentSubscription(UpdateWorkspaceSubscriptionRequest request) {
        return workspaceSubscriptionService.updateCurrentSubscription(request);
    }

    @Transactional
    public BillingWebhookProcessResponse processProviderWebhook(BillingWebhookEventRequest request, String signature) {
        return billingWebhookService.processProviderWebhook(request, signature);
    }

    // ── response transforms ──────────────────────────────────────────────

    private WorkspaceCommercialSummaryResponse toSummary(
            WorkspaceSubscriptionJpaEntity subscription,
            WorkspaceOnboardingProfileJpaEntity onboarding
    ) {
        String normalizedActivationStatus = workspaceOnboardingService.normalizeActivationStatus(onboarding.getActivationStatus());
        return new WorkspaceCommercialSummaryResponse(
                subscription.getPlanCode(),
                subscription.getPlanLabel(),
                subscription.getSubscriptionStatus(),
                subscription.getBillingInterval(),
                subscription.getIncludedCredits(),
                subscription.getExtraCredits(),
                subscription.getIncludedCredits() + subscription.getExtraCredits(),
                subscription.getRenewsAt(),
                onboarding.getPrimaryUseCase(),
                onboarding.getWorkStyle(),
                normalizedActivationStatus,
                onboarding.getActivationNote() == null || onboarding.getActivationNote().isBlank()
                        ? workspaceOnboardingService.defaultActivationNote(normalizedActivationStatus)
                        : onboarding.getActivationNote(),
                subscription.getCommercialNote()
        );
    }

    private InvoiceResponse toInvoiceResponse(WorkspaceInvoiceJpaEntity invoice) {
        return new InvoiceResponse(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getStatus(),
                invoice.getAmountBrl(),
                invoice.getCurrency(),
                invoice.getDueAt() == null ? null : DATE_FORMATTER.format(invoice.getDueAt()),
                invoice.getPaidAt() == null ? null : DATE_TIME_FORMATTER.format(invoice.getPaidAt()),
                invoice.getDescription(),
                invoice.getCreatedAt() == null ? null : DATE_TIME_FORMATTER.format(invoice.getCreatedAt())
        );
    }

    private PaymentEventResponse toPaymentEventResponse(WorkspacePaymentEventJpaEntity paymentEvent) {
        return new PaymentEventResponse(
                paymentEvent.getId(),
                paymentEvent.getInvoice() == null ? null : paymentEvent.getInvoice().getId(),
                paymentEvent.getGatewayEventId(),
                paymentEvent.getEventType(),
                paymentEvent.getStatus(),
                paymentEvent.getAmountBrl(),
                paymentEvent.getOccurredAt() == null ? null : DATE_TIME_FORMATTER.format(paymentEvent.getOccurredAt()),
                paymentEvent.getProcessedAt() == null ? null : DATE_TIME_FORMATTER.format(paymentEvent.getProcessedAt())
        );
    }
}
