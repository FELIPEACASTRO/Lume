package com.lume.workspace.inference.orchestration;

public record AiCostEstimate(
        int estimatedInputTokens,
        int estimatedOutputTokens,
        Double estimatedCostUsd
) {
}
