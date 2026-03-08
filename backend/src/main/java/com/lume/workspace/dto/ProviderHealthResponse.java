package com.lume.workspace.dto;

import java.util.List;

public record ProviderHealthResponse(
        String providerCode,
        String providerName,
        String category,
        boolean configured,
        boolean executionSupported,
        String implementationStatus,
        String evidenceLevel,
        String streamingMode,
        String runtimeMaturity,
        String readinessStatus,
        String healthSource,
        String snapshotPersistence,
        String message,
        String lastConnectivityStatus,
        String lastCheckedAt,
        List<String> missingCredentialEnvVars
) {
}
