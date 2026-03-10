package com.lume.workspace.service;

import com.lume.domain.exception.AccessDeniedException;
import com.lume.domain.exception.BusinessRuleException;
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
import com.lume.workspace.inference.security.SecretResolver;
import com.lume.workspace.repository.WorkspaceInvoiceJpaRepository;
import com.lume.workspace.repository.WorkspaceOnboardingProfileJpaRepository;
import com.lume.workspace.repository.WorkspacePaymentEventJpaRepository;
import com.lume.workspace.repository.WorkspaceSubscriptionJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class WorkspaceCommercialService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final WorkspaceOnboardingProfileJpaRepository onboardingRepository;
    private final WorkspaceSubscriptionJpaRepository subscriptionRepository;
    private final WorkspaceInvoiceJpaRepository invoiceRepository;
    private final WorkspacePaymentEventJpaRepository paymentEventRepository;
    private final WorkspaceContextService workspaceContextService;
    private final AuditLogService auditLogService;
    private final WorkspaceLedgerService workspaceLedgerService;
    private final SecretResolver secretResolver;

    public WorkspaceCommercialService(
            WorkspaceOnboardingProfileJpaRepository onboardingRepository,
            WorkspaceSubscriptionJpaRepository subscriptionRepository,
            WorkspaceInvoiceJpaRepository invoiceRepository,
            WorkspacePaymentEventJpaRepository paymentEventRepository,
            WorkspaceContextService workspaceContextService,
            AuditLogService auditLogService,
            WorkspaceLedgerService workspaceLedgerService,
            SecretResolver secretResolver
    ) {
        this.onboardingRepository = onboardingRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentEventRepository = paymentEventRepository;
        this.workspaceContextService = workspaceContextService;
        this.auditLogService = auditLogService;
        this.workspaceLedgerService = workspaceLedgerService;
        this.secretResolver = secretResolver;
    }

    @Transactional
    public void initializeWorkspace(Long workspaceId, BootstrapSetupRequest request) {
        WorkspaceOnboardingProfileJpaEntity onboarding = getOrCreateOnboarding(workspaceId);
        onboarding.setPrimaryUseCase(normalizePrimaryUseCase(request.primaryUseCase()));
        onboarding.setWorkStyle(normalizeWorkStyle(request.workStyle()));
        onboarding.setActivationStatus("guided_setup");
        onboarding.setActivationNote("Workspace criado com onboarding guiado para o primeiro uso.");
        onboardingRepository.save(onboarding);

        WorkspaceSubscriptionJpaEntity subscription = getOrCreateSubscription(workspaceId);
        applyPlanDefaults(subscription, normalizePlanCode(request.selectedPlan()));
        subscription.setSubscriptionStatus("active");
        subscription.setBillingInterval("monthly");
        subscription.setRenewsAt(LocalDate.now().plusDays(30));
        subscription.setCommercialNote("Plano inicial do workspace. Creditos extras e overage entram em uma trilha posterior.");
        subscriptionRepository.save(subscription);
        workspaceLedgerService.recordCreditEntry(
                workspaceId,
                "grant",
                "subscription_onboarding",
                "workspace:" + workspaceId,
                subscription.getIncludedCredits() + subscription.getExtraCredits(),
                "Creditos iniciais aplicados no bootstrap comercial do workspace."
        );
    }

    public WorkspaceCommercialSummaryResponse getCurrentSummary() {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_WORKSPACE_READ);
        return getSummaryForWorkspace(workspaceContextService.getWorkspaceId());
    }

    public WorkspaceCommercialSummaryResponse getSummaryForWorkspace(Long workspaceId) {
        WorkspaceOnboardingProfileJpaEntity onboarding = getOrCreateOnboarding(workspaceId);
        WorkspaceSubscriptionJpaEntity subscription = getOrCreateSubscription(workspaceId);
        return toSummary(subscription, onboarding);
    }

    public WorkspaceOnboardingResponse getCurrentOnboarding() {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_WORKSPACE_READ);
        return toOnboardingResponse(getOrCreateOnboarding(workspaceContextService.getWorkspaceId()));
    }

    @Transactional
    public WorkspaceOnboardingResponse updateCurrentOnboarding(UpdateWorkspaceOnboardingRequest request) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_SETTINGS_MANAGE);
        WorkspaceOnboardingProfileJpaEntity onboarding = getOrCreateOnboarding(workspaceContextService.getWorkspaceId());

        if (request.primaryUseCase() != null && !request.primaryUseCase().isBlank()) {
            onboarding.setPrimaryUseCase(normalizePrimaryUseCase(request.primaryUseCase()));
        }
        if (request.workStyle() != null && !request.workStyle().isBlank()) {
            onboarding.setWorkStyle(normalizeWorkStyle(request.workStyle()));
        }
        if (request.activationStatus() != null && !request.activationStatus().isBlank()) {
            onboarding.setActivationStatus(normalizeActivationStatus(request.activationStatus()));
        }
        if (request.activationNote() != null && !request.activationNote().isBlank()) {
            onboarding.setActivationNote(request.activationNote().trim());
        }

        WorkspaceOnboardingProfileJpaEntity saved = onboardingRepository.save(onboarding);
        auditLogService.record(
                "workspace_onboarding",
                String.valueOf(saved.getWorkspaceId()),
                "updated",
                Map.of(
                        "primaryUseCase", saved.getPrimaryUseCase(),
                        "workStyle", saved.getWorkStyle(),
                        "activationStatus", saved.getActivationStatus()
                )
        );
        return toOnboardingResponse(saved);
    }

    public WorkspaceSubscriptionResponse getCurrentSubscription() {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_WORKSPACE_READ);
        return toSubscriptionResponse(getOrCreateSubscription(workspaceContextService.getWorkspaceId()));
    }

    @Transactional
    public WorkspaceSubscriptionResponse updateCurrentSubscription(UpdateWorkspaceSubscriptionRequest request) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_SETTINGS_MANAGE);
        WorkspaceSubscriptionJpaEntity subscription = getOrCreateSubscription(workspaceContextService.getWorkspaceId());
        int previousTotalCredits = subscription.getIncludedCredits() + subscription.getExtraCredits();

        if (request.planCode() != null && !request.planCode().isBlank()) {
            applyPlanDefaults(subscription, normalizePlanCode(request.planCode()));
        }
        if (request.subscriptionStatus() != null && !request.subscriptionStatus().isBlank()) {
            subscription.setSubscriptionStatus(normalizeSubscriptionStatus(request.subscriptionStatus()));
        }
        if (request.billingInterval() != null && !request.billingInterval().isBlank()) {
            subscription.setBillingInterval(normalizeBillingInterval(request.billingInterval()));
        }
        if (request.includedCredits() != null) {
            subscription.setIncludedCredits(request.includedCredits());
        }
        if (request.extraCredits() != null) {
            subscription.setExtraCredits(request.extraCredits());
        }
        if (request.renewsAt() != null) {
            subscription.setRenewsAt(request.renewsAt());
        }
        if (request.commercialNote() != null && !request.commercialNote().isBlank()) {
            subscription.setCommercialNote(request.commercialNote().trim());
        }

        validateSubscription(subscription);
        WorkspaceSubscriptionJpaEntity saved = subscriptionRepository.save(subscription);
        int updatedTotalCredits = saved.getIncludedCredits() + saved.getExtraCredits();
        int deltaCredits = updatedTotalCredits - previousTotalCredits;
        if (deltaCredits != 0) {
            workspaceLedgerService.recordCreditEntry(
                    saved.getWorkspaceId(),
                    deltaCredits > 0 ? "adjustment_grant" : "adjustment_debit",
                    "subscription_update",
                    "workspace:" + saved.getWorkspaceId(),
                    deltaCredits,
                    "Ajuste de creditos apos atualizacao da assinatura."
            );
        }
        auditLogService.record(
                "workspace_subscription",
                String.valueOf(saved.getWorkspaceId()),
                "updated",
                Map.of(
                        "planCode", saved.getPlanCode(),
                        "subscriptionStatus", saved.getSubscriptionStatus(),
                        "includedCredits", saved.getIncludedCredits(),
                        "extraCredits", saved.getExtraCredits()
                )
        );
        return toSubscriptionResponse(saved);
    }

    public java.util.List<InvoiceResponse> listCurrentInvoices() {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_WORKSPACE_READ);
        return invoiceRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceContextService.getWorkspaceId())
                .stream()
                .map(this::toInvoiceResponse)
                .toList();
    }

    public java.util.List<PaymentEventResponse> listCurrentPaymentEvents() {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_WORKSPACE_READ);
        return paymentEventRepository.findByWorkspaceIdOrderByOccurredAtDesc(workspaceContextService.getWorkspaceId())
                .stream()
                .map(this::toPaymentEventResponse)
                .toList();
    }

    @Transactional
    public PurchaseCreditPackResponse purchaseCreditPack(PurchaseCreditPackRequest request) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_SETTINGS_MANAGE);
        Long workspaceId = workspaceContextService.getWorkspaceId();

        WorkspaceSubscriptionJpaEntity subscription = getOrCreateSubscription(workspaceId);
        subscription.setExtraCredits(subscription.getExtraCredits() + request.credits());
        validateSubscription(subscription);
        WorkspaceSubscriptionJpaEntity savedSubscription = subscriptionRepository.save(subscription);

        WorkspaceInvoiceJpaEntity invoice = new WorkspaceInvoiceJpaEntity();
        invoice.setWorkspaceId(workspaceId);
        invoice.setInvoiceNumber("INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT));
        invoice.setStatus("paid");
        invoice.setAmountBrl(request.amountBrl().setScale(2, RoundingMode.HALF_UP));
        invoice.setCurrency("BRL");
        invoice.setDueAt(LocalDate.now());
        invoice.setPaidAt(LocalDateTime.now());
        invoice.setDescription(request.description() == null || request.description().isBlank()
                ? "Compra de pack de creditos: " + request.packCode()
                : request.description().trim());
        WorkspaceInvoiceJpaEntity savedInvoice = invoiceRepository.save(invoice);

        WorkspacePaymentEventJpaEntity paymentEvent = new WorkspacePaymentEventJpaEntity();
        paymentEvent.setWorkspaceId(workspaceId);
        paymentEvent.setInvoice(savedInvoice);
        paymentEvent.setGatewayEventId("manual-" + UUID.randomUUID().toString().substring(0, 12));
        paymentEvent.setEventType("credit_pack_purchase");
        paymentEvent.setStatus("processed");
        paymentEvent.setAmountBrl(savedInvoice.getAmountBrl());
        paymentEvent.setPayloadHash("manual_pack_" + request.packCode());
        paymentEvent.setOccurredAt(LocalDateTime.now());
        paymentEvent.setProcessedAt(LocalDateTime.now());
        WorkspacePaymentEventJpaEntity savedPayment = paymentEventRepository.save(paymentEvent);

        workspaceLedgerService.recordCreditEntry(
                workspaceId,
                "pack_purchase",
                "billing_pack",
                savedInvoice.getInvoiceNumber(),
                request.credits(),
                "Compra de pack " + request.packCode() + " registrada no billing."
        );

        workspaceLedgerService.recordUsageEvent(
                "billing.credit_pack_purchased",
                "workspace_subscription",
                String.valueOf(savedSubscription.getWorkspaceId()),
                "Pack " + request.packCode() + " com " + request.credits() + " creditos."
        );

        auditLogService.record(
                "workspace_billing",
                String.valueOf(workspaceId),
                "credit_pack_purchased",
                Map.of(
                        "invoiceNumber", savedInvoice.getInvoiceNumber(),
                        "packCode", request.packCode(),
                        "credits", request.credits()
                )
        );

        return new PurchaseCreditPackResponse(
                toSubscriptionResponse(savedSubscription),
                toInvoiceResponse(savedInvoice),
                toPaymentEventResponse(savedPayment)
        );
    }

    @Transactional
    public BillingWebhookProcessResponse processProviderWebhook(BillingWebhookEventRequest request, String signature) {
        verifyWebhookSignature(request, signature);

        String eventType = normalizeWebhookEventType(request.eventType());
        String eventStatus = normalizeWebhookStatus(request.status());

        var existingEvent = paymentEventRepository.findByWorkspaceIdAndGatewayEventId(
                request.workspaceId(),
                request.gatewayEventId().trim()
        );
        if (existingEvent.isPresent()) {
            WorkspacePaymentEventJpaEntity duplicate = existingEvent.get();
            WorkspaceInvoiceJpaEntity duplicateInvoice = duplicate.getInvoice();
            return new BillingWebhookProcessResponse(
                    duplicate.getGatewayEventId(),
                    "duplicate",
                    true,
                    duplicate.getEventType(),
                    duplicate.getStatus(),
                    duplicateInvoice == null ? null : duplicateInvoice.getInvoiceNumber(),
                    getOrCreateSubscription(request.workspaceId()).getSubscriptionStatus()
            );
        }

        WorkspaceSubscriptionJpaEntity subscription = getOrCreateSubscription(request.workspaceId());
        applySubscriptionPatchFromWebhook(subscription, request, eventType);

        Integer creditsDelta = request.creditsDelta();
        if (creditsDelta != null && creditsDelta != 0) {
            int updatedExtraCredits = subscription.getExtraCredits() + creditsDelta;
            if (updatedExtraCredits < 0) {
                throw new BusinessRuleException("creditsDelta resultou em creditos extras negativos.");
            }
            subscription.setExtraCredits(updatedExtraCredits);
        }

        validateSubscription(subscription);
        WorkspaceSubscriptionJpaEntity savedSubscription = subscriptionRepository.save(subscription);

        WorkspaceInvoiceJpaEntity invoice = upsertInvoiceFromWebhook(request, eventType, eventStatus);

        WorkspacePaymentEventJpaEntity paymentEvent = new WorkspacePaymentEventJpaEntity();
        paymentEvent.setWorkspaceId(request.workspaceId());
        paymentEvent.setInvoice(invoice);
        paymentEvent.setGatewayEventId(request.gatewayEventId().trim());
        paymentEvent.setEventType(eventType);
        paymentEvent.setStatus(eventStatus);
        paymentEvent.setAmountBrl(scaleAmountOrNull(request.amountBrl()));
        paymentEvent.setPayloadHash(sha256Hex(canonicalPayload(request)));
        paymentEvent.setOccurredAt(parseOccurredAt(request.occurredAt()));
        paymentEvent.setProcessedAt(LocalDateTime.now());
        WorkspacePaymentEventJpaEntity savedEvent = paymentEventRepository.save(paymentEvent);

        if (creditsDelta != null && creditsDelta != 0) {
            workspaceLedgerService.recordCreditEntry(
                    request.workspaceId(),
                    creditsDelta > 0 ? "webhook_credit_grant" : "webhook_credit_debit",
                    "billing_webhook",
                    request.gatewayEventId().trim(),
                    creditsDelta,
                    "Ajuste de creditos registrado via webhook de billing."
            );
        }

        workspaceLedgerService.recordUsageEvent(
                "billing.webhook_processed",
                "workspace_payment_event",
                String.valueOf(savedEvent.getId()),
                "Webhook " + eventType + " processado com status " + eventStatus + "."
        );

        Map<String, Object> auditPayload = new LinkedHashMap<>();
        auditPayload.put("gatewayEventId", savedEvent.getGatewayEventId());
        auditPayload.put("eventType", eventType);
        auditPayload.put("status", eventStatus);
        auditPayload.put("invoiceNumber", invoice == null ? null : invoice.getInvoiceNumber());
        auditPayload.put("creditsDelta", creditsDelta);
        auditPayload.put("subscriptionStatus", savedSubscription.getSubscriptionStatus());
        auditLogService.record(
                "workspace_billing",
                String.valueOf(request.workspaceId()),
                "webhook_processed",
                auditPayload
        );

        return new BillingWebhookProcessResponse(
                savedEvent.getGatewayEventId(),
                "processed",
                false,
                savedEvent.getEventType(),
                savedEvent.getStatus(),
                invoice == null ? null : invoice.getInvoiceNumber(),
                savedSubscription.getSubscriptionStatus()
        );
    }

    private WorkspaceOnboardingProfileJpaEntity getOrCreateOnboarding(Long workspaceId) {
        return onboardingRepository.findByWorkspaceId(workspaceId).orElseGet(() -> {
            WorkspaceOnboardingProfileJpaEntity entity = new WorkspaceOnboardingProfileJpaEntity();
            entity.setWorkspaceId(workspaceId);
            return onboardingRepository.save(entity);
        });
    }

    private WorkspaceSubscriptionJpaEntity getOrCreateSubscription(Long workspaceId) {
        return subscriptionRepository.findByWorkspaceId(workspaceId).orElseGet(() -> {
            WorkspaceSubscriptionJpaEntity entity = new WorkspaceSubscriptionJpaEntity();
            entity.setWorkspaceId(workspaceId);
            applyPlanDefaults(entity, "starter");
            entity.setRenewsAt(LocalDate.now().plusDays(30));
            return subscriptionRepository.save(entity);
        });
    }

    private WorkspaceCommercialSummaryResponse toSummary(
            WorkspaceSubscriptionJpaEntity subscription,
            WorkspaceOnboardingProfileJpaEntity onboarding
    ) {
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
                onboarding.getActivationStatus(),
                onboarding.getActivationNote(),
                subscription.getCommercialNote()
        );
    }

    private WorkspaceOnboardingResponse toOnboardingResponse(WorkspaceOnboardingProfileJpaEntity onboarding) {
        return new WorkspaceOnboardingResponse(
                onboarding.getPrimaryUseCase(),
                onboarding.getWorkStyle(),
                onboarding.getActivationStatus(),
                onboarding.getActivationNote()
        );
    }

    private WorkspaceSubscriptionResponse toSubscriptionResponse(WorkspaceSubscriptionJpaEntity subscription) {
        return new WorkspaceSubscriptionResponse(
                subscription.getPlanCode(),
                subscription.getPlanLabel(),
                subscription.getSubscriptionStatus(),
                subscription.getBillingInterval(),
                subscription.getIncludedCredits(),
                subscription.getExtraCredits(),
                subscription.getIncludedCredits() + subscription.getExtraCredits(),
                subscription.getRenewsAt(),
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

    private WorkspaceInvoiceJpaEntity upsertInvoiceFromWebhook(
            BillingWebhookEventRequest request,
            String eventType,
            String eventStatus
    ) {
        String invoiceNumber = request.invoiceNumber() == null ? null : request.invoiceNumber().trim();
        boolean invoiceEvent = isInvoiceEvent(eventType);

        if ((invoiceNumber == null || invoiceNumber.isBlank()) && !invoiceEvent) {
            return null;
        }
        if ((invoiceNumber == null || invoiceNumber.isBlank()) && invoiceEvent) {
            throw new BusinessRuleException("invoiceNumber e obrigatorio para eventos de fatura.");
        }

        WorkspaceInvoiceJpaEntity invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber).orElseGet(() -> {
            WorkspaceInvoiceJpaEntity created = new WorkspaceInvoiceJpaEntity();
            created.setWorkspaceId(request.workspaceId());
            created.setInvoiceNumber(invoiceNumber);
            created.setStatus("open");
            created.setAmountBrl(scaleAmountOrZero(request.amountBrl()));
            created.setCurrency(normalizeCurrency(request.currency()));
            created.setDescription(request.description() == null ? null : request.description().trim());
            created.setDueAt(parseLocalDateOrNull(request.dueAt(), "dueAt"));
            return created;
        });

        if (!request.workspaceId().equals(invoice.getWorkspaceId())) {
            throw new BusinessRuleException("invoiceNumber pertence a outro workspace.");
        }
        if (request.amountBrl() != null) {
            invoice.setAmountBrl(scaleAmountOrZero(request.amountBrl()));
        }
        if (request.currency() != null && !request.currency().isBlank()) {
            invoice.setCurrency(normalizeCurrency(request.currency()));
        }
        if (request.description() != null && !request.description().isBlank()) {
            invoice.setDescription(request.description().trim());
        }
        if (request.dueAt() != null && !request.dueAt().isBlank()) {
            invoice.setDueAt(parseLocalDateOrNull(request.dueAt(), "dueAt"));
        }

        applyInvoiceStatus(invoice, eventType, eventStatus);
        return invoiceRepository.save(invoice);
    }

    private void applyInvoiceStatus(WorkspaceInvoiceJpaEntity invoice, String eventType, String eventStatus) {
        switch (eventType) {
            case "invoice_paid", "credit_pack_purchased", "payment_succeeded" -> {
                invoice.setStatus("paid");
                invoice.setPaidAt(LocalDateTime.now());
            }
            case "invoice_payment_failed", "payment_failed" -> {
                invoice.setStatus("payment_failed");
                invoice.setPaidAt(null);
            }
            case "invoice_created" -> {
                if (invoice.getStatus() == null || invoice.getStatus().isBlank()) {
                    invoice.setStatus("open");
                }
            }
            default -> {
                if ("canceled".equals(eventStatus)) {
                    invoice.setStatus("canceled");
                    invoice.setPaidAt(null);
                } else if ("pending".equals(eventStatus)) {
                    invoice.setStatus("pending");
                }
            }
        }
    }

    private void applySubscriptionPatchFromWebhook(
            WorkspaceSubscriptionJpaEntity subscription,
            BillingWebhookEventRequest request,
            String eventType
    ) {
        if (request.planCode() != null && !request.planCode().isBlank()) {
            applyPlanDefaults(subscription, normalizePlanCode(request.planCode()));
        }
        if (request.subscriptionStatus() != null && !request.subscriptionStatus().isBlank()) {
            subscription.setSubscriptionStatus(normalizeSubscriptionStatus(request.subscriptionStatus()));
        }
        if (request.billingInterval() != null && !request.billingInterval().isBlank()) {
            subscription.setBillingInterval(normalizeBillingInterval(request.billingInterval()));
        }
        if (request.includedCredits() != null) {
            subscription.setIncludedCredits(request.includedCredits());
        }
        if (request.extraCredits() != null) {
            subscription.setExtraCredits(request.extraCredits());
        }
        if (request.renewsAt() != null && !request.renewsAt().isBlank()) {
            subscription.setRenewsAt(parseLocalDateOrNull(request.renewsAt(), "renewsAt"));
        } else if ("subscription_renewed".equals(eventType)) {
            subscription.setRenewsAt("annual".equals(subscription.getBillingInterval())
                    ? LocalDate.now().plusDays(365)
                    : LocalDate.now().plusDays(30));
            subscription.setSubscriptionStatus("active");
        }
        if ("subscription_canceled".equals(eventType)) {
            subscription.setSubscriptionStatus("canceled");
        }
        if (request.commercialNote() != null && !request.commercialNote().isBlank()) {
            subscription.setCommercialNote(request.commercialNote().trim());
        }
    }

    private void verifyWebhookSignature(BillingWebhookEventRequest request, String signatureHeader) {
        String webhookSecret = secretResolver.resolveOptional("BILLING_WEBHOOK_SECRET");
        if (webhookSecret == null) {
            throw new BusinessRuleException("Webhook de billing indisponivel: BILLING_WEBHOOK_SECRET nao configurada.");
        }
        if (signatureHeader == null || signatureHeader.isBlank()) {
            throw new AccessDeniedException("Assinatura do webhook ausente.");
        }

        String providedSignature = signatureHeader.trim().toLowerCase(Locale.ROOT);
        if (providedSignature.startsWith("sha256=")) {
            providedSignature = providedSignature.substring("sha256=".length());
        }

        String expectedSignature = hmacSha256Hex(webhookSecret, canonicalSignaturePayload(request));
        boolean matches = MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                providedSignature.getBytes(StandardCharsets.UTF_8)
        );
        if (!matches) {
            throw new AccessDeniedException("Assinatura do webhook invalida.");
        }
    }

    private String canonicalSignaturePayload(BillingWebhookEventRequest request) {
        return String.join("|",
                safeCanonical(request.gatewayEventId()),
                String.valueOf(request.workspaceId()),
                safeCanonical(request.eventType()),
                safeCanonical(request.status()),
                safeCanonical(request.invoiceNumber()),
                safeCanonical(scaleAmountOrNull(request.amountBrl()) == null ? null : scaleAmountOrNull(request.amountBrl()).toPlainString()),
                safeCanonical(request.currency())
        );
    }

    private String canonicalPayload(BillingWebhookEventRequest request) {
        return String.join("|",
                safeCanonical(request.gatewayEventId()),
                safeCanonical(request.eventType()),
                safeCanonical(request.status()),
                safeCanonical(request.invoiceNumber()),
                safeCanonical(request.description()),
                safeCanonical(request.dueAt()),
                safeCanonical(request.occurredAt()),
                safeCanonical(request.planCode()),
                safeCanonical(request.subscriptionStatus()),
                safeCanonical(request.billingInterval()),
                safeCanonical(request.renewsAt()),
                request.includedCredits() == null ? "" : String.valueOf(request.includedCredits()),
                request.extraCredits() == null ? "" : String.valueOf(request.extraCredits()),
                request.creditsDelta() == null ? "" : String.valueOf(request.creditsDelta())
        );
    }

    private String hmacSha256Hex(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                builder.append(String.format(Locale.ROOT, "%02x", b));
            }
            return builder.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao calcular assinatura do webhook de billing.", ex);
        }
    }

    private String sha256Hex(String payload) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                builder.append(String.format(Locale.ROOT, "%02x", b));
            }
            return builder.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao calcular hash do payload de billing.", ex);
        }
    }

    private LocalDateTime parseOccurredAt(String value) {
        if (value == null || value.isBlank()) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(value.trim(), DATE_TIME_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new BusinessRuleException("occurredAt deve usar formato ISO-8601 sem timezone (yyyy-MM-ddTHH:mm:ss).");
        }
    }

    private LocalDate parseLocalDateOrNull(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim(), DATE_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new BusinessRuleException(fieldName + " deve usar formato ISO-8601 (yyyy-MM-dd).");
        }
    }

    private boolean isInvoiceEvent(String eventType) {
        return switch (eventType) {
            case "invoice_created", "invoice_paid", "invoice_payment_failed", "credit_pack_purchased", "payment_succeeded", "payment_failed" -> true;
            default -> false;
        };
    }

    private BigDecimal scaleAmountOrNull(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal scaleAmountOrZero(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeCurrency(String value) {
        if (value == null || value.isBlank()) {
            return "BRL";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeWebhookEventType(String value) {
        String normalized = normalizeWebhookToken(value);
        return switch (normalized) {
            case "invoice_created", "invoice_paid", "invoice_payment_failed", "subscription_updated", "subscription_renewed",
                    "subscription_canceled", "credit_pack_purchased", "payment_succeeded", "payment_failed" -> normalized;
            default -> throw new BusinessRuleException("eventType de webhook nao suportado para billing.");
        };
    }

    private String normalizeWebhookStatus(String value) {
        String normalized = normalizeWebhookToken(value);
        return switch (normalized) {
            case "paid", "processed", "succeeded", "ok" -> "processed";
            case "pending", "processing", "in_progress" -> "pending";
            case "failed", "error", "payment_failed" -> "failed";
            case "canceled", "cancelled" -> "canceled";
            default -> throw new BusinessRuleException("status de webhook invalido para billing.");
        };
    }

    private String safeCanonical(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeWebhookToken(String value) {
        return value == null
                ? ""
                : value.trim()
                .toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_')
                .replace('.', '_');
    }

    private void applyPlanDefaults(WorkspaceSubscriptionJpaEntity subscription, String planCode) {
        switch (planCode) {
            case "core" -> {
                subscription.setPlanCode("core");
                subscription.setPlanLabel("Core");
                subscription.setIncludedCredits(1500);
            }
            case "pro" -> {
                subscription.setPlanCode("pro");
                subscription.setPlanLabel("Pro");
                subscription.setIncludedCredits(5000);
            }
            default -> {
                subscription.setPlanCode("starter");
                subscription.setPlanLabel("Starter");
                subscription.setIncludedCredits(500);
            }
        }
    }

    private void validateSubscription(WorkspaceSubscriptionJpaEntity subscription) {
        if (subscription.getIncludedCredits() < 0 || subscription.getExtraCredits() < 0) {
            throw new BusinessRuleException("Os creditos comerciais nao podem ser negativos.");
        }
        normalizePlanCode(subscription.getPlanCode());
        normalizeSubscriptionStatus(subscription.getSubscriptionStatus());
        normalizeBillingInterval(subscription.getBillingInterval());
    }

    private String normalizePrimaryUseCase(String value) {
        String normalized = normalizeToken(value);
        return switch (normalized) {
            case "operations", "research", "support", "content", "analysis" -> normalized;
            default -> throw new BusinessRuleException("primaryUseCase deve ser operations, research, support, content ou analysis.");
        };
    }

    private String normalizeWorkStyle(String value) {
        String normalized = normalizeToken(value);
        return switch (normalized) {
            case "solo_operator", "small_team", "department_team", "multi_team" -> normalized;
            default -> throw new BusinessRuleException("workStyle deve ser solo_operator, small_team, department_team ou multi_team.");
        };
    }

    private String normalizeActivationStatus(String value) {
        String normalized = normalizeToken(value);
        return switch (normalized) {
            case "guided_setup", "active", "paused", "ready" -> normalized;
            default -> throw new BusinessRuleException("activationStatus deve ser guided_setup, active, paused ou ready.");
        };
    }

    private String normalizePlanCode(String value) {
        String normalized = normalizeToken(value);
        return switch (normalized) {
            case "starter", "core", "pro" -> normalized;
            default -> throw new BusinessRuleException("planCode deve ser starter, core ou pro.");
        };
    }

    private String normalizeSubscriptionStatus(String value) {
        String normalized = normalizeToken(value);
        return switch (normalized) {
            case "active", "trialing", "past_due", "paused", "canceled" -> normalized;
            default -> throw new BusinessRuleException("subscriptionStatus deve ser active, trialing, past_due, paused ou canceled.");
        };
    }

    private String normalizeBillingInterval(String value) {
        String normalized = normalizeToken(value);
        return switch (normalized) {
            case "monthly", "annual" -> normalized;
            default -> throw new BusinessRuleException("billingInterval deve ser monthly ou annual.");
        };
    }

    private String normalizeToken(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }
}
