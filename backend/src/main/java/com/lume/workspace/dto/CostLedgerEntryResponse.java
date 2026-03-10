package com.lume.workspace.dto;

public record CostLedgerEntryResponse(
        Long id,
        String providerCode,
        String modelCode,
        String capability,
        String requestId,
        String status,
        Integer estimatedInputTokens,
        Integer estimatedOutputTokens,
        Double estimatedCostUsd,
        Long latencyMs,
        boolean fallbackUsed,
        String routingMode,
        PolicyDecisionSummaryResponse policyDecisionSummary,
        String createdAt
) {
}
