package com.lume.workspace.inference.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lume.workspace.inference.ModelDefinition;
import com.lume.workspace.inference.adapter.AbstractAiProviderAdapter;
import com.lume.workspace.inference.orchestration.AiPromptCommand;

@FunctionalInterface
public interface RequestShapeStrategy {

    JsonNode buildPayload(
            AiPromptCommand command,
            ModelDefinition model,
            AbstractAiProviderAdapter adapter,
            ObjectMapper objectMapper
    );
}
