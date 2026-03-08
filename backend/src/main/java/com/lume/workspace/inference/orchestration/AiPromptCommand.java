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
        Integer maxTokens
) {
}
