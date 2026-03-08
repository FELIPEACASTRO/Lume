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
        String implementationStatus,
        String evidenceLevel,
        String businessPriority,
        String syncMode,
        boolean adminOnly,
        boolean tenantScoped,
        boolean supportsResponsesApi,
        boolean supportsChatCompletions,
        String streamingMode,
        String runtimeMaturity,
        String catalogState,
        String pricingSummary,
        String rateLimitSummary,
        List<String> routingModes,
        String documentationSource,
        List<String> requiredHeaders,
        List<CredentialFieldResponse> credentialFields,
        String apiKeyPortalUrl,
        String docsUrl,
        String defaultModelCode,
        List<String> capabilities,
        String notes
) {
}
