package com.lume.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lume.workspace.dto.BillingWebhookEventRequest;
import com.lume.workspace.repository.WorkspaceCreditLedgerEntryJpaRepository;
import com.lume.workspace.repository.WorkspaceInvoiceJpaRepository;
import com.lume.workspace.repository.WorkspacePaymentEventJpaRepository;
import com.lume.workspace.repository.WorkspaceSubscriptionJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Mercado Pago webhook - Integration Tests")
class MercadoPagoWebhookIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WorkspacePaymentEventJpaRepository paymentEventRepository;

    @Autowired
    private WorkspaceInvoiceJpaRepository invoiceRepository;

    @Autowired
    private WorkspaceSubscriptionJpaRepository subscriptionRepository;

    @Autowired
    private WorkspaceCreditLedgerEntryJpaRepository creditLedgerEntryRepository;

    @Test
    @DisplayName("POST /api/v1/billing/webhooks/mercado-pago - should process idempotently")
    void shouldProcessMercadoPagoWebhookIdempotently() throws Exception {
        Long workspaceId = 10L;
        int ledgerCreditsBefore = creditLedgerEntryRepository.sumCreditsByWorkspaceId(workspaceId);
        long invoicesBefore = invoiceRepository.countByWorkspaceId(workspaceId);
        long paymentEventsBefore = paymentEventRepository.countByWorkspaceId(workspaceId);

        BillingWebhookEventRequest request = new BillingWebhookEventRequest(
                workspaceId,
                "evt-mp-001",
                "invoice.paid",
                "paid",
                "INV-MP-001",
                new BigDecimal("149.90"),
                "BRL",
                "Pagamento de pack de creditos via Mercado Pago",
                "2026-04-08",
                freshOccurredAt(),
                "core",
                "active",
                "monthly",
                1500,
                0,
                "2026-04-11",
                "Credito aplicado por webhook Mercado Pago.",
                75
        );

        String signature = hmacSha256Hex("test-billing-webhook-secret", canonicalSignaturePayload(request));

        mockMvc.perform(post("/v1/billing/webhooks/mercado-pago")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-MercadoPago-Signature", signature)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processingStatus").value("processed"))
                .andExpect(jsonPath("$.duplicate").value(false))
                .andExpect(jsonPath("$.invoiceNumber").value("INV-MP-001"));

        int subscriptionCreditsAfterFirstCall = totalSubscriptionCredits(workspaceId);
        int ledgerCreditsAfterFirstCall = creditLedgerEntryRepository.sumCreditsByWorkspaceId(workspaceId);
        long invoicesAfterFirstCall = invoiceRepository.countByWorkspaceId(workspaceId);
        long paymentEventsAfterFirstCall = paymentEventRepository.countByWorkspaceId(workspaceId);

        var invoiceAfterFirstCall = invoiceRepository.findByInvoiceNumber("INV-MP-001");
        assertThat(invoiceAfterFirstCall).isPresent();
        assertThat(invoiceAfterFirstCall.get().getStatus()).isEqualTo("paid");

        mockMvc.perform(post("/v1/billing/webhooks/mercado-pago")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-MercadoPago-Signature", signature)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processingStatus").value("duplicate"))
                .andExpect(jsonPath("$.duplicate").value(true))
                .andExpect(jsonPath("$.invoiceNumber").value("INV-MP-001"));

        int subscriptionCreditsAfterReplay = totalSubscriptionCredits(workspaceId);
        int ledgerCreditsAfterReplay = creditLedgerEntryRepository.sumCreditsByWorkspaceId(workspaceId);
        long invoicesAfterReplay = invoiceRepository.countByWorkspaceId(workspaceId);
        long paymentEventsAfterReplay = paymentEventRepository.countByWorkspaceId(workspaceId);

        var invoiceAfterReplay = invoiceRepository.findByInvoiceNumber("INV-MP-001");
        assertThat(invoiceAfterReplay).isPresent();
        assertThat(invoiceAfterReplay.get().getStatus()).isEqualTo("paid");
        assertThat(invoiceAfterReplay.get().getId()).isEqualTo(invoiceAfterFirstCall.get().getId());

        var paymentEvent = paymentEventRepository.findByWorkspaceIdAndGatewayEventId(workspaceId, "evt-mp-001");
        assertThat(paymentEvent).isPresent();
        assertThat(paymentEvent.get().getEventType()).isEqualTo("invoice_paid");
        assertThat(paymentEvent.get().getStatus()).isEqualTo("processed");

        assertThat(invoicesAfterFirstCall).isEqualTo(invoicesBefore + 1);
        assertThat(paymentEventsAfterFirstCall).isEqualTo(paymentEventsBefore + 1);
        assertThat(subscriptionCreditsAfterFirstCall).isEqualTo(1575);
        assertThat(ledgerCreditsAfterFirstCall).isEqualTo(ledgerCreditsBefore + 75);

        assertThat(invoicesAfterReplay).isEqualTo(invoicesAfterFirstCall);
        assertThat(paymentEventsAfterReplay).isEqualTo(paymentEventsAfterFirstCall);
        assertThat(subscriptionCreditsAfterReplay).isEqualTo(subscriptionCreditsAfterFirstCall);
        assertThat(ledgerCreditsAfterReplay).isEqualTo(ledgerCreditsAfterFirstCall);
    }

    @Test
    @DisplayName("POST /api/v1/billing/webhooks/mercado-pago - should reject invalid signature")
    void shouldRejectMercadoPagoWebhookWithInvalidSignature() throws Exception {
        BillingWebhookEventRequest request = new BillingWebhookEventRequest(
                10L,
                "evt-mp-invalid-signature",
                "invoice.paid",
                "paid",
                "INV-MP-INVALID",
                new BigDecimal("79.90"),
                "BRL",
                "Assinatura invalida no webhook Mercado Pago",
                "2026-04-10",
                freshOccurredAt(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        mockMvc.perform(post("/v1/billing/webhooks/mercado-pago")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-MercadoPago-Signature", "invalid-signature")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Assinatura do webhook invalida."));
    }

    @Test
    @DisplayName("POST /api/v1/billing/webhooks/mercado-pago - should reject unsupported status")
    void shouldRejectMercadoPagoWebhookWithUnsupportedStatus() throws Exception {
        BillingWebhookEventRequest request = new BillingWebhookEventRequest(
                10L,
                "evt-mp-invalid-status",
                "invoice.paid",
                "mystery",
                "INV-MP-INVALID-STATUS",
                new BigDecimal("59.90"),
                "BRL",
                "Status invalido enviado pelo webhook Mercado Pago",
                "2026-04-10",
                freshOccurredAt(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        String signature = hmacSha256Hex("test-billing-webhook-secret", canonicalSignaturePayload(request));

        mockMvc.perform(post("/v1/billing/webhooks/mercado-pago")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-MercadoPago-Signature", signature)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("status de webhook invalido para billing."));
    }

    @Test
    @DisplayName("POST /api/v1/billing/webhooks/mercado-pago - should reject invalid payload")
    void shouldRejectMercadoPagoWebhookWithInvalidPayload() throws Exception {
        BillingWebhookEventRequest request = new BillingWebhookEventRequest(
                10L,
                "",
                "invoice.paid",
                "paid",
                "INV-MP-VALIDATION",
                new BigDecimal("39.90"),
                "BRL",
                "Payload invalido",
                "2026-04-10",
                freshOccurredAt(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        mockMvc.perform(post("/v1/billing/webhooks/mercado-pago")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-MercadoPago-Signature", "any-signature")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Erro de validacao"))
                .andExpect(jsonPath("$.details.gatewayEventId").exists());
    }

    private String canonicalSignaturePayload(BillingWebhookEventRequest request) {
        return String.join("|",
                safeCanonical(request.gatewayEventId()),
                String.valueOf(request.workspaceId()),
                safeCanonical(request.eventType()),
                safeCanonical(request.status()),
                safeCanonical(request.invoiceNumber()),
                request.amountBrl() == null ? "" : request.amountBrl().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(),
                safeCanonical(request.currency())
        );
    }

    private String safeCanonical(String value) {
        return value == null ? "" : value.trim();
    }

    private String hmacSha256Hex(String secret, String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            builder.append(String.format(Locale.ROOT, "%02x", b));
        }
        return builder.toString();
    }

    private int totalSubscriptionCredits(Long workspaceId) {
        var subscription = subscriptionRepository.findByWorkspaceId(workspaceId).orElseThrow();
        return subscription.getIncludedCredits() + subscription.getExtraCredits();
    }

    private String freshOccurredAt() {
        return LocalDateTime.now()
                .minusSeconds(5)
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}

