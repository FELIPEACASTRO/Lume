package com.lume.workspace.dto;

public record BudgetSummaryResponse(
        String costCenter,
        String chargebackMode,
        int softLimitCredits,
        int hardLimitCredits,
        int consumedCredits,
        int remainingSoftCredits,
        int remainingHardCredits,
        int softLimitUtilizationPercent,
        int hardLimitUtilizationPercent,
        boolean softLimitReached,
        boolean hardLimitReached,
        String budgetStatus,
        String note
) {
}
