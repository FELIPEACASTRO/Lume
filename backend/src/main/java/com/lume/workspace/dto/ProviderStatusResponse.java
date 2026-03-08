package com.lume.workspace.dto;

import java.util.List;

public record ProviderStatusResponse(
        String providerCode,
        String providerName,
        boolean configured,
        boolean executionSupported,
        String catalogState,
        String category,
        boolean adminOnly,
        List<String> missingCredentialEnvVars
) {
}
