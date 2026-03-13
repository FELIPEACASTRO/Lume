package com.lume.workspace.dto;

import java.util.List;

public record ProviderReadinessResponse(
        String providerCode,
        String providerName,
        String category,
        boolean configured,
        boolean executionSupported,
        String providerTier,
        String readinessStatus,
        String runtimeMaturity,
        String implementationStatus,
        String evidenceLevel,
        String smokeStatus,
        String blockerCode,
        String blockerMessage,
        boolean adminOnly,
        List<String> missingCredentialEnvVars
) {
}
