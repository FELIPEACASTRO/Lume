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
public class OpenAiAdapter extends AbstractAiProviderAdapter {

    public OpenAiAdapter(
            ProviderCatalogService providerCatalogService,
            AiHttpExecutor httpExecutor,
            AiRuntimeProperties runtimeProperties,
            ObjectMapper objectMapper
    ) {
        super(providerCatalogService, httpExecutor, runtimeProperties, objectMapper);
    }

    @Override
    public ProviderKey providerKey() {
        return ProviderKey.OPENAI;
    }

    @Override
    public AiPromptResult sendPrompt(AiPromptCommand command) {
        ModelDefinition model = modelFor(command.modelCode());
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", externalModelCode(model.code()));
        payload.put("temperature", command.temperature() != null ? command.temperature() : 0.3);
        payload.put("max_output_tokens", command.maxTokens() != null ? command.maxTokens() : 700);
        payload.set("input", buildInput(command));

        JsonNode response = postJson(
                baseUrl() + "/responses",
                Map.of("Authorization", "Bearer " + providerCatalogService.credentialValue(provider(), "apiKey")),
                payload
        );

        return buildResult(command, model, extractResponseContent(response));
    }

    private ArrayNode buildInput(AiPromptCommand command) {
        ArrayNode input = objectMapper.createArrayNode();

        if (command.systemPrompt() != null && !command.systemPrompt().isBlank()) {
            input.add(objectMapper.createObjectNode()
                    .put("role", "system")
                    .put("content", command.systemPrompt().trim()));
        }

        normalizeMessages(command).forEach(message -> input.add(objectMapper.createObjectNode()
                .put("role", normalizeRole(message.role()))
                .put("content", message.content())));

        return input;
    }

    private String extractResponseContent(JsonNode response) {
        StringBuilder content = new StringBuilder();
        if (response == null) {
            return "";
        }
        for (JsonNode item : response.path("output")) {
            for (JsonNode contentItem : item.path("content")) {
                if ("output_text".equalsIgnoreCase(contentItem.path("type").asText())) {
                    content.append(contentItem.path("text").asText(""));
                }
            }
        }
        if (content.isEmpty()) {
            content.append(response.path("output_text").asText(""));
        }
        return content.toString();
    }
}
