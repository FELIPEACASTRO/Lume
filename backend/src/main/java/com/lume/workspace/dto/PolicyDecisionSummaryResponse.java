package com.lume.workspace.dto;

public record PolicyDecisionSummaryResponse(
        String routingMode,
        String requestedProviderCode,
        String selectedProviderCode,
        boolean fallbackUsed,
        String status,
        String reason
) {
}
