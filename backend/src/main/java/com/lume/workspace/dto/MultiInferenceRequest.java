package com.lume.workspace.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record MultiInferenceRequest(
        @NotEmpty @Valid List<MultiInferenceExecutionTargetRequest> executionSet,
        String systemPrompt,
        String prompt,
        @Valid List<UnifiedMessageRequest> messages,
        Double temperature,
        Integer maxTokens,
        String rankingPolicy,
        String routingPolicy,
        String requestId,
        List<String> tags,
        String workspaceId
) {
}
