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
        String requestId,
        String routingMode,
        Boolean stream,
        List<String> tags,
        String workspaceId
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
        this(providerCode, modelCode, systemPrompt, prompt, messages, temperature, maxTokens, List.of(), null, null, null, List.of(), null);
    }

    public UnifiedInferenceRequest(
            String providerCode,
            String modelCode,
            String systemPrompt,
            String prompt,
            List<UnifiedMessageRequest> messages,
            Double temperature,
            Integer maxTokens,
            List<String> fallbackProviderCodes,
            String requestId
    ) {
        this(providerCode, modelCode, systemPrompt, prompt, messages, temperature, maxTokens, fallbackProviderCodes, requestId, null, null, List.of(), null);
    }
}
