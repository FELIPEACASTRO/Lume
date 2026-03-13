package com.lume.workspace.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lume.domain.exception.BusinessRuleException;
import com.lume.workspace.inference.security.SecretResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class MercadoPagoCheckoutAdapter {

    private static final String DEFAULT_API_BASE_URL = "https://api.mercadopago.com";

    private final SecretResolver secretResolver;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public MercadoPagoCheckoutAdapter(
            SecretResolver secretResolver,
            ObjectMapper objectMapper,
            HttpClient httpClient
    ) {
        this.secretResolver = secretResolver;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Autowired
    public MercadoPagoCheckoutAdapter(
            SecretResolver secretResolver,
            ObjectMapper objectMapper
    ) {
        this(secretResolver, objectMapper, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build());
    }

    public MercadoPagoCheckoutSession createCreditPackCheckout(
            Long workspaceId,
            String invoiceNumber,
            String packCode,
            int credits,
            BigDecimal amountBrl,
            String description
    ) {
        String accessToken = secretResolver.resolveOptional("MERCADO_PAGO_ACCESS_TOKEN");
        if (accessToken == null || accessToken.isBlank()) {
            throw new BusinessRuleException("Mercado Pago indisponivel: MERCADO_PAGO_ACCESS_TOKEN nao configurada.");
        }

        String apiBaseUrl = secretResolver.resolveOptional("MERCADO_PAGO_API_BASE_URL");
        if (apiBaseUrl == null || apiBaseUrl.isBlank()) {
            apiBaseUrl = DEFAULT_API_BASE_URL;
        }

        String notificationUrl = secretResolver.resolveOptional("MERCADO_PAGO_NOTIFICATION_URL");
        String successUrl = secretResolver.resolveOptional("MERCADO_PAGO_SUCCESS_URL");
        String pendingUrl = secretResolver.resolveOptional("MERCADO_PAGO_PENDING_URL");
        String failureUrl = secretResolver.resolveOptional("MERCADO_PAGO_FAILURE_URL");

        String externalReference = "workspace:" + workspaceId + "|invoice:" + invoiceNumber + "|pack:" + packCode;
        String itemTitle = description == null || description.isBlank()
                ? "Pack de creditos " + packCode + " (" + credits + " creditos)"
                : description.trim();

        Map<String, Object> payload = Map.of(
                "external_reference", externalReference,
                "items", List.of(
                        Map.of(
                                "title", itemTitle,
                                "quantity", 1,
                                "currency_id", "BRL",
                                "unit_price", amountBrl.doubleValue()
                        )
                ),
                "back_urls", Map.of(
                        "success", successUrl == null ? "" : successUrl,
                        "pending", pendingUrl == null ? "" : pendingUrl,
                        "failure", failureUrl == null ? "" : failureUrl
                ),
                "notification_url", notificationUrl == null ? "" : notificationUrl,
                "auto_return", "approved"
        );

        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(payload);
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao serializar payload de checkout Mercado Pago.", ex);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiBaseUrl + "/checkout/preferences"))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + accessToken.trim())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Requisicao Mercado Pago interrompida.", ex);
        } catch (IOException ex) {
            throw new IllegalStateException("Falha de comunicacao com Mercado Pago.", ex);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new BusinessRuleException(
                    "Falha ao criar checkout no Mercado Pago. status="
                            + response.statusCode()
                            + " body="
                            + truncate(response.body())
            );
        }

        try {
            JsonNode jsonNode = objectMapper.readTree(response.body());
            String preferenceId = textValue(jsonNode, "id");
            String checkoutUrl = textValue(jsonNode, "init_point");
            if (preferenceId == null || preferenceId.isBlank() || checkoutUrl == null || checkoutUrl.isBlank()) {
                throw new BusinessRuleException("Resposta invalida do Mercado Pago ao criar checkout.");
            }
            return new MercadoPagoCheckoutSession(preferenceId, checkoutUrl, externalReference, "pending");
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao interpretar resposta do Mercado Pago.", ex);
        }
    }

    private String textValue(JsonNode node, String fieldName) {
        if (node == null || !node.has(fieldName) || node.get(fieldName).isNull()) {
            return null;
        }
        return node.get(fieldName).asText();
    }

    private String truncate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() > 300 ? value.substring(0, 300) : value;
    }

    public record MercadoPagoCheckoutSession(
            String preferenceId,
            String checkoutUrl,
            String externalReference,
            String paymentStatus
    ) {
    }
}
