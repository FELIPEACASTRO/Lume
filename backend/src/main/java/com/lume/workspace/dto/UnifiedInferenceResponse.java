package com.lume.workspace.dto;

public record UnifiedInferenceResponse(
        String providerCode,
        String providerName,
        String modelCode,
        String versionLabel,
        boolean configured,
        boolean executionSupported,
        boolean fallbackUsed,
        String status,
        String content,
        String error
) {
}
