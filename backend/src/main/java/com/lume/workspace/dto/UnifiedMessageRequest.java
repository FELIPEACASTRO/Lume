package com.lume.workspace.dto;

import jakarta.validation.constraints.NotBlank;

public record UnifiedMessageRequest(
        @NotBlank String role,
        @NotBlank String content
) {
}
