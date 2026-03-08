package com.lume.workspace.dto;

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
        String versionLabel
) {
}
