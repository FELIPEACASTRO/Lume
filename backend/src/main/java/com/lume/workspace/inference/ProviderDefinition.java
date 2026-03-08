package com.lume.workspace.inference;

import java.util.List;

public record ProviderDefinition(
        String code,
        String name,
        String category,
        InferenceProtocol protocol,
        boolean executionSupported,
        String baseUrlTemplate,
        List<CredentialFieldDefinition> credentialFields,
        String authScheme,
        String apiStyle,
        List<String> requiredHeaders,
        boolean adminOnly,
        boolean supportsResponsesApi,
        boolean supportsChatCompletions,
        boolean tenantScoped,
        String catalogState,
        String apiKeyPortalUrl,
        String docsUrl,
        String defaultModelCode,
        List<String> capabilities,
        String notes
) {
}
