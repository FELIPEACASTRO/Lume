package com.lume.workspace.dto;

public record AgentThreadResponse(
        String id,
        String agentProfileId,
        String agentName,
        String title,
        String status,
        String availability,
        String lastMessagePreview,
        String updatedAt,
        String providerCode,
        String modelCode,
        String versionLabel,
        String apiStyle,
        String credentialState,
        String catalogState
) {
}
