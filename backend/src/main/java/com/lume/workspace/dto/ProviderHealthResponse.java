package com.lume.workspace.dto;

import java.util.List;

public record ProviderHealthResponse(
        String providerCode,
        String providerName,
        String category,
        boolean configured,
        boolean executionSupported,
        boolean streamingSupported,
        String readinessStatus,
        String message,
        String lastConnectivityStatus,
        String lastCheckedAt,
        List<String> missingCredentialEnvVars
) {
}
