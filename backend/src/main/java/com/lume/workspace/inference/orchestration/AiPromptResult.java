package com.lume.workspace.inference.orchestration;

public record AiPromptResult(
        String providerCode,
        String providerName,
        String modelCode,
        String versionLabel,
        String apiStyle,
        String content,
        AiCostEstimate costEstimate,
        boolean streamingSupported
) {
}
