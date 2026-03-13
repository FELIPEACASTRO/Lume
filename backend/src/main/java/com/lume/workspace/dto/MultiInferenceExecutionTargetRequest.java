package com.lume.workspace.dto;

import jakarta.validation.constraints.NotBlank;

public record MultiInferenceExecutionTargetRequest(
        @NotBlank String providerCode,
        @NotBlank String modelCode,
        String versionLabel
) {
}
