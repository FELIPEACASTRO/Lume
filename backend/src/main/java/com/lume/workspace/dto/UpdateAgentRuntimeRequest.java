package com.lume.workspace.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateAgentRuntimeRequest(
        @NotBlank String providerCode,
        String modelCode,
        String versionLabel,
        String systemPrompt
) {
}
