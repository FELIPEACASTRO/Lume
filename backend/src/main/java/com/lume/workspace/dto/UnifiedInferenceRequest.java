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
        Integer maxTokens,
        List<String> fallbackProviderCodes,
        String requestId
) {

    public UnifiedInferenceRequest(
            String providerCode,
            String modelCode,
            String systemPrompt,
            String prompt,
            List<UnifiedMessageRequest> messages,
            Double temperature,
            Integer maxTokens
    ) {
        this(providerCode, modelCode, systemPrompt, prompt, messages, temperature, maxTokens, List.of(), null);
    }
}
