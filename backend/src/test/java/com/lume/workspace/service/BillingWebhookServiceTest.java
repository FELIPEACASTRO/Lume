package com.lume.workspace.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lume.domain.exception.AccessDeniedException;
import com.lume.workspace.dto.BillingWebhookEventRequest;
import com.lume.workspace.dto.BillingWebhookProcessResponse;
import com.lume.workspace.entity.WorkspaceInvoiceJpaEntity;
import com.lume.workspace.entity.WorkspacePaymentEventJpaEntity;
import com.lume.workspace.entity.WorkspaceSubscriptionJpaEntity;
import com.lume.workspace.inference.security.SecretResolver;
import com.lume.workspace.repository.WorkspaceInvoiceJpaRepository;
import com.lume.workspace.repository.WorkspacePaymentEventJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("BillingWebhookService - Unit Tests")
class BillingWebhookServiceTest {

    private static final String WEBHOOK_SECRET = "test-secret-key-12345";

    private WorkspaceInvoiceJpaRepository invoiceRepository;
    private WorkspacePaymentEventJpaRepository paymentEventRepository;
    private SpyAuditLogService auditLogService;
    private SecretResolver secretResolver;
    private BillingWebhookService service;

    @BeforeEach
    void setUp() {
        invoiceRepository = mock(WorkspaceInvoiceJpaRepository.class);
        paymentEventRepository = mock(WorkspacePaymentEventJpaRepository.class);
        auditLogService = new SpyAuditLogService();
        secretResolver = mock(SecretResolver.class);

        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service = new BillingWebhookService(
                invoiceRepository,
                paymentEventRepository,
                new StubWorkspaceSubscriptionService(),
                auditLogService,
                new NoOpWorkspaceLedgerService(),
                secretResolver
        );
    }

    @Test
    @DisplayName("processProviderWebhook rejects missing signature")
    void shouldRejectWebhookWithMissingSignature() {
        when(secretResolver.resolveOptional("BILLING_WEBHOOK_SECRET")).thenReturn(WEBHOOK_SECRET);

        BillingWebhookEventRequest request = webhookRequest("invoice_paid", "succeeded");

        assertThatThrownBy(() -> service.processProviderWebhook(request, null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("ausente");
    }

    @Test
    @DisplayName("processProviderWebhook rejects invalid HMAC signature")
    void shouldRejectWebhookWithInvalidHmac() {
        when(secretResolver.resolveOptional("BILLING_WEBHOOK_SECRET")).thenReturn(WEBHOOK_SECRET);

        BillingWebhookEventRequest request = webhookRequest("invoice_paid", "succeeded");

        assertThatThrownBy(() -> service.processProviderWebhook(request, "sha256=deadbeef"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("invalida");
    }

    @Test
    @DisplayName("processProviderWebhook processes valid webhook and sets invoice to paid")
    void shouldProcessValidWebhookAndSetInvoicePaid() {
        when(secretResolver.resolveOptional("BILLING_WEBHOOK_SECRET")).thenReturn(WEBHOOK_SECRET);
        when(paymentEventRepository.findByWorkspaceIdAndGatewayEventId(1L, "gw-event-1"))
                .thenReturn(Optional.empty());
        when(invoiceRepository.findByInvoiceNumber("INV-001"))
                .thenReturn(Optional.of(defaultInvoice()));

        BillingWebhookEventRequest request = webhookRequest("invoice_paid", "succeeded");
        String validSignature = computeSignature(request);

        BillingWebhookProcessResponse response = service.processProviderWebhook(request, "sha256=" + validSignature);

        assertThat(response.processingStatus()).isEqualTo("processed");
        assertThat(response.duplicate()).isFalse();
        assertThat(response.eventType()).isEqualTo("invoice_paid");
        assertThat(auditLogService.recordedActions).contains("webhook_processed");
    }

    @Test
    @DisplayName("processProviderWebhook returns duplicate when event already exists")
    void shouldReturnDuplicateForExistingEvent() {
        when(secretResolver.resolveOptional("BILLING_WEBHOOK_SECRET")).thenReturn(WEBHOOK_SECRET);

        WorkspacePaymentEventJpaEntity existing = new WorkspacePaymentEventJpaEntity();
        existing.setGatewayEventId("gw-event-1");
        existing.setEventType("invoice_paid");
        existing.setStatus("succeeded");
        when(paymentEventRepository.findByWorkspaceIdAndGatewayEventId(1L, "gw-event-1"))
                .thenReturn(Optional.of(existing));

        BillingWebhookEventRequest request = webhookRequest("invoice_paid", "succeeded");
        String validSignature = computeSignature(request);

        BillingWebhookProcessResponse response = service.processProviderWebhook(request, "sha256=" + validSignature);

        assertThat(response.processingStatus()).isEqualTo("duplicate");
        assertThat(response.duplicate()).isTrue();
    }

    @Test
    @DisplayName("processProviderWebhook applies subscription_canceled status")
    void shouldApplySubscriptionCanceledFromWebhook() {
        when(secretResolver.resolveOptional("BILLING_WEBHOOK_SECRET")).thenReturn(WEBHOOK_SECRET);
        when(paymentEventRepository.findByWorkspaceIdAndGatewayEventId(1L, "gw-event-1"))
                .thenReturn(Optional.empty());

        BillingWebhookEventRequest request = new BillingWebhookEventRequest(
                1L, "gw-event-1", "subscription_canceled", "canceled",
                null, null, null, null, null, null,
                null, null, null, null, null, null, null, null
        );
        String validSignature = computeSignature(request);

        BillingWebhookProcessResponse response = service.processProviderWebhook(request, "sha256=" + validSignature);

        assertThat(response.subscriptionStatus()).isEqualTo("canceled");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private BillingWebhookEventRequest webhookRequest(String eventType, String status) {
        return new BillingWebhookEventRequest(
                1L, "gw-event-1", eventType, status,
                "INV-001", new BigDecimal("49.90"), "BRL",
                "Test invoice", null, null,
                null, null, null, null, null, null, null, null
        );
    }

    private String computeSignature(BillingWebhookEventRequest request) {
        String payload = String.join("|",
                safe(request.gatewayEventId()),
                String.valueOf(request.workspaceId()),
                safe(request.eventType()),
                safe(request.status()),
                safe(request.invoiceNumber()),
                request.amountBrl() == null ? "" : request.amountBrl().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(),
                safe(request.currency())
        );
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String safe(String v) {
        return v == null ? "" : v.trim();
    }

    private WorkspaceInvoiceJpaEntity defaultInvoice() {
        WorkspaceInvoiceJpaEntity entity = new WorkspaceInvoiceJpaEntity();
        entity.setWorkspaceId(1L);
        entity.setInvoiceNumber("INV-001");
        entity.setStatus("open");
        entity.setAmountBrl(new BigDecimal("49.90"));
        entity.setCurrency("BRL");
        return entity;
    }

    // ── test doubles ────────────────────────────────────────────────────────

    private static final class SpyAuditLogService extends AuditLogService {
        final List<String> recordedActions = new ArrayList<>();

        private SpyAuditLogService() {
            super(null, null, new ObjectMapper());
        }

        @Override
        public void record(String entityType, String entityId, String action, Object payload) {
            recordedActions.add(action);
        }

        @Override
        public void recordExplicit(Long orgId, Long wsId, Long userId,
                                   String entityType, String entityId, String action, Object payload) {
            recordedActions.add(action);
        }
    }

    private static final class StubWorkspaceSubscriptionService extends WorkspaceSubscriptionService {
        private StubWorkspaceSubscriptionService() {
            super(null, null, null, null);
        }

        @Override
        public WorkspaceSubscriptionJpaEntity getOrCreateSubscription(Long workspaceId) {
            WorkspaceSubscriptionJpaEntity entity = new WorkspaceSubscriptionJpaEntity();
            entity.setWorkspaceId(workspaceId);
            entity.setPlanCode("starter");
            entity.setPlanLabel("Starter");
            entity.setSubscriptionStatus("active");
            entity.setBillingInterval("monthly");
            entity.setIncludedCredits(100);
            entity.setExtraCredits(0);
            entity.setRenewsAt(LocalDate.now().plusDays(30));
            entity.setCommercialNote("Plano inicial.");
            return entity;
        }

        @Override
        public void validateSubscription(WorkspaceSubscriptionJpaEntity subscription) {
            // no-op for tests
        }
    }
}
