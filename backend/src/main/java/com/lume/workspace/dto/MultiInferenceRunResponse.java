package com.lume.workspace.dto;

import com.lume.workspace.inference.orchestration.AiExecutionAttempt;

import java.util.List;

public record MultiInferenceRunResponse(
        String providerCode,
        String providerName,
        String modelCode,
        String versionLabel,
        String status,
        String content,
        String error,
        boolean fallbackUsed,
        Long latencyMs,
        Integer estimatedInputTokens,
        Integer estimatedOutputTokens,
        Double estimatedCostUsd,
        Double score,
        List<String> rankingReasons,
        List<AiExecutionAttempt> attemptChain
) {
}
