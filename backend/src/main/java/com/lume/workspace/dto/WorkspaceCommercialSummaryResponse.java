package com.lume.workspace.dto;

import java.time.LocalDate;

public record WorkspaceCommercialSummaryResponse(
        String planCode,
        String planLabel,
        String subscriptionStatus,
        String billingInterval,
        int includedCredits,
        int extraCredits,
        int totalCredits,
        LocalDate renewsAt,
        String primaryUseCase,
        String workStyle,
        String activationStatus,
        String activationNote,
        String commercialNote
) {
}
