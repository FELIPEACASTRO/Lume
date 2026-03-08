package com.lume.workspace.dto;

import java.util.List;

public record ProviderConnectivityResponse(
        String providerCode,
        String providerName,
        String category,
        String apiStyle,
        String status,
        boolean configured,
        boolean executionSupported,
        boolean streamingSupported,
        Long latencyMs,
        String message,
        List<String> missingCredentialEnvVars
) {
}
