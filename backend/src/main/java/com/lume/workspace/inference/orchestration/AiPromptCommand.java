package com.lume.workspace.inference.orchestration;

import java.util.List;

public record AiPromptCommand(
        String requestId,
        String requestedProviderCode,
        String providerCode,
        String modelCode,
        String versionLabel,
        String systemPrompt,
        List<AiMessage> messages,
        Double temperature,
        Integer maxTokens,
        String routingMode,
        List<String> tags,
        String workspaceId
) {
    public AiPromptCommand(
            String requestId,
            String requestedProviderCode,
            String providerCode,
            String modelCode,
            String versionLabel,
            String systemPrompt,
            List<AiMessage> messages,
            Double temperature,
            Integer maxTokens
    ) {
        this(requestId, requestedProviderCode, providerCode, modelCode, versionLabel, systemPrompt, messages, temperature, maxTokens, null, List.of(), null);
    }
}
