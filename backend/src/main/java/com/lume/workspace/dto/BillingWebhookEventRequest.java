package com.lume.workspace.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record BillingWebhookEventRequest(
        @NotNull(message = "workspaceId e obrigatorio")
        Long workspaceId,
        @NotBlank(message = "gatewayEventId e obrigatorio")
        @Size(max = 128, message = "gatewayEventId nao pode ultrapassar 128 caracteres")
        String gatewayEventId,
        @NotBlank(message = "eventType e obrigatorio")
        @Size(max = 64, message = "eventType nao pode ultrapassar 64 caracteres")
        String eventType,
        @NotBlank(message = "status e obrigatorio")
        @Size(max = 32, message = "status nao pode ultrapassar 32 caracteres")
        String status,
        @Size(max = 64, message = "invoiceNumber nao pode ultrapassar 64 caracteres")
        String invoiceNumber,
        BigDecimal amountBrl,
        @Size(max = 8, message = "currency nao pode ultrapassar 8 caracteres")
        String currency,
        @Size(max = 255, message = "description nao pode ultrapassar 255 caracteres")
        String description,
        String dueAt,
        String occurredAt,
        @Size(max = 32, message = "planCode nao pode ultrapassar 32 caracteres")
        String planCode,
        @Size(max = 32, message = "subscriptionStatus nao pode ultrapassar 32 caracteres")
        String subscriptionStatus,
        @Size(max = 32, message = "billingInterval nao pode ultrapassar 32 caracteres")
        String billingInterval,
        @Min(value = 0, message = "includedCredits nao pode ser negativo")
        Integer includedCredits,
        @Min(value = 0, message = "extraCredits nao pode ser negativo")
        Integer extraCredits,
        String renewsAt,
        @Size(max = 255, message = "commercialNote nao pode ultrapassar 255 caracteres")
        String commercialNote,
        Integer creditsDelta
) {
}
