package com.lume.workspace.dto;

import java.time.LocalDate;

public record WorkspaceSubscriptionResponse(
        String planCode,
        String planLabel,
        String subscriptionStatus,
        String billingInterval,
        int includedCredits,
        int extraCredits,
        int totalCredits,
        LocalDate renewsAt,
        String commercialNote
) {
}
