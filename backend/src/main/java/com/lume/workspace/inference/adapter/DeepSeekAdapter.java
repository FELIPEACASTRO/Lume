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
public class DeepSeekAdapter extends AbstractAiProviderAdapter {

    public DeepSeekAdapter(
            ProviderCatalogService providerCatalogService,
            AiHttpExecutor httpExecutor,
            AiRuntimeProperties runtimeProperties,
            ObjectMapper objectMapper
    ) {
        super(providerCatalogService, httpExecutor, runtimeProperties, objectMapper);
    }

    @Override
    public ProviderKey providerKey() {
        return ProviderKey.DEEPSEEK;
    }

    @Override
    public AiPromptResult sendPrompt(AiPromptCommand command) {
        ModelDefinition model = modelFor(command.modelCode());
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", externalModelCode(model.code()));
        payload.put("temperature", command.temperature() != null ? command.temperature() : 0.3);
        payload.put("max_tokens", command.maxTokens() != null ? command.maxTokens() : 700);
        payload.set("messages", buildMessages(command));

        JsonNode response = postJson(
                baseUrl() + "/chat/completions",
                Map.of("Authorization", "Bearer " + providerCatalogService.credentialValue(provider(), "apiKey")),
                payload
        );

        return buildResult(command, model, extractContent(response));
    }

    private ArrayNode buildMessages(AiPromptCommand command) {
        ArrayNode messages = objectMapper.createArrayNode();
        if (command.systemPrompt() != null && !command.systemPrompt().isBlank()) {
            messages.add(objectMapper.createObjectNode()
                    .put("role", "system")
                    .put("content", command.systemPrompt().trim()));
        }
        normalizeMessages(command).forEach(message -> messages.add(objectMapper.createObjectNode()
                .put("role", normalizeRole(message.role()))
                .put("content", message.content())));
        return messages;
    }

    private String extractContent(JsonNode response) {
        JsonNode contentNode = response.path("choices").path(0).path("message").path("content");
        if (contentNode.isTextual()) {
            return contentNode.asText("");
        }
        StringBuilder content = new StringBuilder();
        contentNode.forEach(node -> content.append(node.path("text").asText(node.asText(""))));
        return content.toString();
    }
}
