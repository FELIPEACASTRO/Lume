package com.lume.workspace.dto;

import java.util.List;

public record AgentProfileResponse(
        String id,
        String name,
        String specialty,
        String description,
        String status,
        String availability,
        String note,
        String providerCode,
        String modelCode,
        String versionLabel,
        String apiStyle,
        String credentialState,
        String catalogState,
        boolean configured,
        boolean executionSupported,
        List<String> toolset
) {
}
