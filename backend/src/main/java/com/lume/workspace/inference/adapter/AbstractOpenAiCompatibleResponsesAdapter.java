package com.lume.workspace.inference.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lume.workspace.inference.ModelDefinition;
import com.lume.workspace.inference.config.AiRuntimeProperties;
import com.lume.workspace.inference.orchestration.AiHttpExecutor;
import com.lume.workspace.service.ProviderCatalogService;

import java.util.Map;

public abstract class AbstractOpenAiCompatibleResponsesAdapter extends AbstractStrategyBackedAdapter {

    protected AbstractOpenAiCompatibleResponsesAdapter(
            ProviderCatalogService providerCatalogService,
            AiHttpExecutor httpExecutor,
            AiRuntimeProperties runtimeProperties,
            ObjectMapper objectMapper
    ) {
        super(
                providerCatalogService,
                httpExecutor,
                runtimeProperties,
                objectMapper,
                (provider, catalogService) -> Map.of("Authorization", "Bearer " + catalogService.credentialValue(provider, "apiKey")),
                (command, model, adapter, mapper) -> {
                    ObjectNode payload = mapper.createObjectNode();
                    payload.put("model", adapter.externalModelCode(model.code()));
                    payload.put("temperature", command.temperature() != null ? command.temperature() : 0.3);
                    payload.put("max_output_tokens", command.maxTokens() != null ? command.maxTokens() : 700);
                    ArrayNode input = mapper.createArrayNode();
                    if (command.systemPrompt() != null && !command.systemPrompt().isBlank()) {
                        input.add(mapper.createObjectNode()
                                .put("role", "system")
                                .put("content", command.systemPrompt().trim()));
                    }
                    adapter.normalizeMessages(command).forEach(message -> input.add(mapper.createObjectNode()
                            .put("role", adapter.normalizeRole(message.role()))
                            .put("content", message.content())));
                    payload.set("input", input);
                    return payload;
                },
                response -> {
                    StringBuilder content = new StringBuilder();
                    if (response != null) {
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
                    }
                    return content.toString();
                }
        );
    }

    @Override
    protected String operationPath(ModelDefinition model) {
        return baseUrl() + "/responses";
    }
}
