package com.lume.workspace.dto;

public record ModelResponse(
        String code,
        String providerCode,
        String label,
        String versionLabel,
        String apiStyle,
        String catalogState,
        boolean defaultModel,
        boolean enabledForAgents
) {
}
