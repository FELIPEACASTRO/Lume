package com.lume.workspace.inference;

import java.util.List;

public record ProviderDefinition(
        String code,
        String name,
        String category,
        InferenceProtocol protocol,
        boolean executionSupported,
        String baseUrl,
        String apiKeyEnvVar,
        String apiKeyPortalUrl,
        String docsUrl,
        String defaultModelCode,
        List<String> capabilities,
        String notes
) {
}
