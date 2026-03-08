package com.lume.workspace.dto;

public record ProviderCredentialResponse(
        String providerCode,
        String providerName,
        String apiKeyEnvVar,
        boolean configured,
        boolean executionSupported,
        String apiKeyPortalUrl,
        String docsUrl
) {
}
