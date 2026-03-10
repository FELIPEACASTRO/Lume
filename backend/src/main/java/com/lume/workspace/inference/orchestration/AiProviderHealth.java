package com.lume.workspace.inference.orchestration;

import java.util.List;

public record AiProviderHealth(
        String providerCode,
        String providerName,
        String category,
        boolean configured,
        boolean executionSupported,
        boolean streamingSupported,
        String readinessStatus,
        String message,
        List<String> missingCredentialEnvVars
) {
}
