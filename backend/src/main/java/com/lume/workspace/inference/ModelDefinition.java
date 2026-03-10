package com.lume.workspace.inference;

public record ModelDefinition(
        String code,
        String providerCode,
        String label,
        String versionLabel,
        boolean defaultModel,
        boolean enabledForAgents
) {
}
