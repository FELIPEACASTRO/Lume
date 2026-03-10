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
        String runtimeMaturity,
        String readinessStatus,
        List<String> missingCredentialEnvVars
) {
}
