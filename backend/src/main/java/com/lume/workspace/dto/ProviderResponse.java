package com.lume.workspace.dto;

import java.util.List;

public record ProviderResponse(
        String code,
        String name,
        String category,
        String protocol,
        boolean executionSupported,
        boolean configured,
        String apiKeyEnvVar,
        String apiKeyPortalUrl,
        String docsUrl,
        String defaultModelCode,
        List<String> capabilities,
        String notes
) {
}
