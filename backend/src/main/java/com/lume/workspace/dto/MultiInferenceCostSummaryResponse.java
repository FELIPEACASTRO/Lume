package com.lume.workspace.dto;

public record MultiInferenceCostSummaryResponse(
        Double totalEstimatedCostUsd,
        Double averageEstimatedCostUsd,
        Integer completedRuns,
        Integer failedRuns
) {
}
