package com.lume.workspace.dto;

import java.util.List;

public record UnifiedInferenceResponse(
        String providerCode,
        String providerName,
        String modelCode,
        String versionLabel,
        String apiStyle,
        boolean configured,
        boolean executionSupported,
        boolean fallbackUsed,
        String status,
        String content,
        String error,
        String requestedProviderCode,
        List<String> attemptedProviderCodes,
        Long latencyMs,
        Integer estimatedInputTokens,
        Integer estimatedOutputTokens,
        Double estimatedCostUsd,
        boolean streamingSupported
) {
}
