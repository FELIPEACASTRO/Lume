package com.lume.workspace.inference.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lume.workspace.inference.ModelDefinition;
import com.lume.workspace.inference.ProviderKey;
import com.lume.workspace.inference.config.AiRuntimeProperties;
import com.lume.workspace.inference.orchestration.AiHttpExecutor;
import com.lume.workspace.inference.orchestration.AiPromptCommand;
import com.lume.workspace.inference.orchestration.AiPromptResult;
import com.lume.workspace.service.ProviderCatalogService;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GeminiAdapter extends AbstractAiProviderAdapter {

    public GeminiAdapter(
            ProviderCatalogService providerCatalogService,
            AiHttpExecutor httpExecutor,
            AiRuntimeProperties runtimeProperties,
            ObjectMapper objectMapper
    ) {
        super(providerCatalogService, httpExecutor, runtimeProperties, objectMapper);
    }

    @Override
    public ProviderKey providerKey() {
        return ProviderKey.GEMINI;
    }

    @Override
    public AiPromptResult sendPrompt(AiPromptCommand command) {
        ModelDefinition model = modelFor(command.modelCode());
        ObjectNode payload = objectMapper.createObjectNode();
        if (command.systemPrompt() != null && !command.systemPrompt().isBlank()) {
            ObjectNode instruction = objectMapper.createObjectNode();
            instruction.set("parts", objectMapper.createArrayNode().add(
                    objectMapper.createObjectNode().put("text", command.systemPrompt().trim())
            ));
            payload.set("system_instruction", instruction);
        }
        payload.set("contents", buildContents(command));

        ObjectNode generationConfig = objectMapper.createObjectNode();
        generationConfig.put("temperature", command.temperature() != null ? command.temperature() : 0.3);
        generationConfig.put("maxOutputTokens", command.maxTokens() != null ? command.maxTokens() : 700);
        payload.set("generationConfig", generationConfig);

        JsonNode response = postJson(
                baseUrl() + "/models/" + externalModelCode(model.code()) + ":generateContent",
                Map.of("x-goog-api-key", providerCatalogService.credentialValue(provider(), "apiKey")),
                payload
        );

        StringBuilder content = new StringBuilder();
        response.path("candidates").forEach(candidate -> candidate.path("content").path("parts")
                .forEach(part -> content.append(part.path("text").asText(""))));

        return buildResult(command, model, content.toString());
    }

    private ArrayNode buildContents(AiPromptCommand command) {
        ArrayNode contents = objectMapper.createArrayNode();
        normalizeMessages(command).forEach(message -> {
            ObjectNode content = objectMapper.createObjectNode();
            content.put("role", "assistant".equalsIgnoreCase(message.role()) ? "model" : "user");
            content.set("parts", objectMapper.createArrayNode().add(
                    objectMapper.createObjectNode().put("text", message.content())
            ));
            contents.add(content);
        });
        return contents;
    }
}
