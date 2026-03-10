package com.lume.workspace.dto;

public record FinopsScorecardResponse(
        Integer activationMinutesToFirstTaskMedian,
        Double d30RetentionRate,
        Double churnRate,
        Double marginContributionRate,
        Integer operationalNps,
        Double taskCompletionRate,
        Double inferenceSuccessRate,
        Double averageEstimatedCostUsdPerRun,
        int currentCreditBalance,
        String notes
) {
}
