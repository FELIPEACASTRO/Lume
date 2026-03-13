package com.lume.workspace.dto;

import java.util.List;

public record ProviderStatusResponse(
        String providerCode,
        String providerName,
        boolean configured,
        boolean executionSupported,
        String implementationStatus,
        String evidenceLevel,
        String catalogState,
        String category,
        boolean adminOnly,
        String streamingMode,
        String providerTier,
        String runtimeMaturity,
        String readinessStatus,
        String smokeStatus,
        String blockerCode,
        String blockerMessage,
        List<String> missingCredentialEnvVars
) {
}
