package com.lume.workspace.dto;

import java.util.List;

public record ProviderResponse(
        String code,
        String name,
        String category,
        String protocol,
        String apiStyle,
        boolean executionSupported,
        boolean configured,
        boolean adminOnly,
        boolean tenantScoped,
        boolean supportsResponsesApi,
        boolean supportsChatCompletions,
        boolean streamingSupported,
        String catalogState,
        List<String> requiredHeaders,
        List<CredentialFieldResponse> credentialFields,
        String apiKeyPortalUrl,
        String docsUrl,
        String defaultModelCode,
        List<String> capabilities,
        String notes
) {
}
