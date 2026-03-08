package com.lume.workspace.dto;

import java.util.List;

public record ProviderCredentialResponse(
        String providerCode,
        String providerName,
        boolean configured,
        boolean executionSupported,
        String category,
        String apiStyle,
        boolean adminOnly,
        boolean streamingSupported,
        String catalogState,
        List<String> missingCredentialEnvVars,
        List<CredentialFieldResponse> credentialFields,
        String apiKeyPortalUrl,
        String docsUrl
) {
}
