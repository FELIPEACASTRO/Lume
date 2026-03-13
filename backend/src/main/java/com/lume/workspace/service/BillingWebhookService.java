package com.lume.workspace.service;

import com.lume.domain.exception.AccessDeniedException;
import com.lume.domain.exception.BusinessRuleException;
import com.lume.workspace.dto.BillingWebhookEventRequest;
import com.lume.workspace.dto.BillingWebhookProcessResponse;
import com.lume.workspace.entity.WorkspaceInvoiceJpaEntity;
import com.lume.workspace.entity.WorkspacePaymentEventJpaEntity;
import com.lume.workspace.entity.WorkspaceSubscriptionJpaEntity;
import com.lume.workspace.inference.security.SecretResolver;
import com.lume.workspace.repository.WorkspaceInvoiceJpaRepository;
import com.lume.workspace.repository.WorkspacePaymentEventJpaRepository;
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

@Service
public class BillingWebhookService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final long WEBHOOK_TIMESTAMP_TOLERANCE_SECONDS = 300; // 5 minutes

    private final WorkspaceInvoiceJpaRepository invoiceRepository;
    private final WorkspacePaymentEventJpaRepository paymentEventRepository;
    private final WorkspaceSubscriptionService workspaceSubscriptionService;
    private final AuditLogService auditLogService;
    private final WorkspaceLedgerService workspaceLedgerService;
    private final SecretResolver secretResolver;

    public BillingWebhookService(
            WorkspaceInvoiceJpaRepository invoiceRepository,
            WorkspacePaymentEventJpaRepository paymentEventRepository,
            WorkspaceSubscriptionService workspaceSubscriptionService,
            AuditLogService auditLogService,
            WorkspaceLedgerService workspaceLedgerService,
            SecretResolver secretResolver
    ) {
        this.invoiceRepository = invoiceRepository;
        this.paymentEventRepository = paymentEventRepository;
        this.workspaceSubscriptionService = workspaceSubscriptionService;
        this.auditLogService = auditLogService;
        this.workspaceLedgerService = workspaceLedgerService;
        this.secretResolver = secretResolver;
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
                    workspaceSubscriptionService.getOrCreateSubscription(request.workspaceId()).getSubscriptionStatus()
            );
        }

        WorkspaceSubscriptionJpaEntity subscription = workspaceSubscriptionService.getOrCreateSubscription(request.workspaceId());
        applySubscriptionPatchFromWebhook(subscription, request, eventType);

        Integer creditsDelta = request.creditsDelta();
        if (creditsDelta != null && creditsDelta != 0) {
            int updatedExtraCredits = subscription.getExtraCredits() + creditsDelta;
            if (updatedExtraCredits < 0) {
                throw new BusinessRuleException("creditsDelta resultou em creditos extras negativos.");
            }
            subscription.setExtraCredits(updatedExtraCredits);
        }

        workspaceSubscriptionService.validateSubscription(subscription);

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
        auditPayload.put("subscriptionStatus", subscription.getSubscriptionStatus());
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
                subscription.getSubscriptionStatus()
        );
    }

    // ── webhook signature verification ───────────────────────────────────

    private void verifyWebhookSignature(BillingWebhookEventRequest request, String signatureHeader) {
        String webhookSecret = secretResolver.resolveOptional("BILLING_WEBHOOK_SECRET");
        if (webhookSecret == null) {
            throw new BusinessRuleException("Webhook de billing indisponivel: BILLING_WEBHOOK_SECRET nao configurada.");
        }
        if (signatureHeader == null || signatureHeader.isBlank()) {
            throw new AccessDeniedException("Assinatura do webhook ausente.");
        }

        rejectExpiredWebhookTimestamp(request.occurredAt());

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

    private void rejectExpiredWebhookTimestamp(String occurredAt) {
        if (occurredAt == null || occurredAt.isBlank()) {
            return;
        }
        try {
            LocalDateTime eventTime = LocalDateTime.parse(occurredAt, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            LocalDateTime now = LocalDateTime.now();
            long diffSeconds = Math.abs(java.time.Duration.between(eventTime, now).getSeconds());
            if (diffSeconds > WEBHOOK_TIMESTAMP_TOLERANCE_SECONDS) {
                throw new AccessDeniedException("Webhook expirado: occurredAt fora da janela de tolerancia de " + WEBHOOK_TIMESTAMP_TOLERANCE_SECONDS + "s.");
            }
        } catch (DateTimeParseException ignored) {
            // non-parseable timestamp — do not block, signature is primary protection
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

    // ── crypto helpers ───────────────────────────────────────────────────

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

    String sha256Hex(String payload) {
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

    // ── invoice helpers ──────────────────────────────────────────────────

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
            workspaceSubscriptionService.applyPlanDefaults(subscription, workspaceSubscriptionService.normalizePlanCode(request.planCode()));
        }
        if (request.subscriptionStatus() != null && !request.subscriptionStatus().isBlank()) {
            subscription.setSubscriptionStatus(workspaceSubscriptionService.normalizeSubscriptionStatus(request.subscriptionStatus()));
        }
        if (request.billingInterval() != null && !request.billingInterval().isBlank()) {
            subscription.setBillingInterval(workspaceSubscriptionService.normalizeBillingInterval(request.billingInterval()));
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

    // ── parsing/normalization helpers ─────────────────────────────────────

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
}
