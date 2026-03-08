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
public class AnthropicAdapter extends AbstractAiProviderAdapter {

    public AnthropicAdapter(
            ProviderCatalogService providerCatalogService,
            AiHttpExecutor httpExecutor,
            AiRuntimeProperties runtimeProperties,
            ObjectMapper objectMapper
    ) {
        super(providerCatalogService, httpExecutor, runtimeProperties, objectMapper);
    }

    @Override
    public ProviderKey providerKey() {
        return ProviderKey.ANTHROPIC;
    }

    @Override
    public AiPromptResult sendPrompt(AiPromptCommand command) {
        ModelDefinition model = modelFor(command.modelCode());
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", externalModelCode(model.code()));
        payload.put("max_tokens", command.maxTokens() != null ? command.maxTokens() : 700);
        payload.put("temperature", command.temperature() != null ? command.temperature() : 0.3);
        if (command.systemPrompt() != null && !command.systemPrompt().isBlank()) {
            payload.put("system", command.systemPrompt().trim());
        }
        payload.set("messages", buildMessages(command));

        JsonNode response = postJson(
                baseUrl() + "/messages",
                Map.of(
                        "x-api-key", providerCatalogService.credentialValue(provider(), "apiKey"),
                        "anthropic-version", "2023-06-01"
                ),
                payload
        );

        StringBuilder content = new StringBuilder();
        response.path("content").forEach(item -> {
            if ("text".equalsIgnoreCase(item.path("type").asText())) {
                content.append(item.path("text").asText(""));
            }
        });

        return buildResult(command, model, content.toString());
    }

    private ArrayNode buildMessages(AiPromptCommand command) {
        ArrayNode messages = objectMapper.createArrayNode();
        normalizeMessages(command).forEach(message -> messages.add(objectMapper.createObjectNode()
                .put("role", "assistant".equalsIgnoreCase(message.role()) ? "assistant" : "user")
                .set("content", objectMapper.createArrayNode().add(
                        objectMapper.createObjectNode()
                                .put("type", "text")
                                .put("text", message.content())
                ))));
        return messages;
    }
}
