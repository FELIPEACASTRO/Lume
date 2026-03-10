package com.lume.workspace.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateWorkspaceSubscriptionRequest(
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
        LocalDate renewsAt,
        @Size(max = 255, message = "commercialNote nao pode ultrapassar 255 caracteres")
        String commercialNote
) {
}
