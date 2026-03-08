package com.lume.workspace.dto;

import com.lume.workspace.inference.orchestration.AiExecutionAttempt;

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
        List<AiExecutionAttempt> attemptChain,
        Long latencyMs,
        Integer estimatedInputTokens,
        Integer estimatedOutputTokens,
        Double estimatedCostUsd,
        String streamingMode,
        String routingMode
) {
}
