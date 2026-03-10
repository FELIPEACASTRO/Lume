package com.lume.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record ResearchQueryRequest(
        @NotBlank String providerCode,
        @NotBlank String query,
        @Positive Integer limit
) {
}
