package com.lume.workspace.dto;

import java.util.List;

public record ProviderCredentialResponse(
        String providerCode,
        String providerName,
        boolean configured,
        boolean executionSupported,
        String implementationStatus,
        String evidenceLevel,
        String businessPriority,
        String syncMode,
        String category,
        String apiStyle,
        boolean adminOnly,
        String streamingMode,
        String runtimeMaturity,
        String catalogState,
        String pricingSummary,
        String rateLimitSummary,
        List<String> missingCredentialEnvVars,
        List<CredentialFieldResponse> credentialFields,
        String apiKeyPortalUrl,
        String docsUrl,
        Boolean freeTierRecurring,
        String freeTierScope,
        String billingWarning,
        List<String> freeModels,
        List<String> regionConstraints
) {
}
