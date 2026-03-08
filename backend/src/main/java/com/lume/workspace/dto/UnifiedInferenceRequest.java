package com.lume.workspace.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record UnifiedInferenceRequest(
        @NotBlank String providerCode,
        String modelCode,
        String systemPrompt,
        String prompt,
        @Valid List<UnifiedMessageRequest> messages,
        Double temperature,
        Integer maxTokens
) {
}
