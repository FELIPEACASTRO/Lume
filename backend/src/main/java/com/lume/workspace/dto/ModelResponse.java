package com.lume.workspace.dto;

public record ModelResponse(
        String code,
        String providerCode,
        String label,
        String versionLabel,
        boolean defaultModel,
        boolean enabledForAgents
) {
}
