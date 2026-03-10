package com.lume.workspace.inference.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lume.workspace.inference.ModelDefinition;
import com.lume.workspace.inference.config.AiRuntimeProperties;
import com.lume.workspace.inference.orchestration.AiHttpExecutor;
import com.lume.workspace.service.ProviderCatalogService;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class AbstractNativeAnthropicAdapter extends AbstractStrategyBackedAdapter {

    protected AbstractNativeAnthropicAdapter(
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
                (provider, catalogService) -> {
                    Map<String, String> headers = new LinkedHashMap<>();
                    headers.put("x-api-key", catalogService.credentialValue(provider, "apiKey"));
                    headers.put("anthropic-version", "2023-06-01");
                    return headers;
                },
                (command, model, adapter, mapper) -> {
                    ObjectNode payload = mapper.createObjectNode();
                    payload.put("model", adapter.externalModelCode(model.code()));
                    payload.put("max_tokens", command.maxTokens() != null ? command.maxTokens() : 700);
                    payload.put("temperature", command.temperature() != null ? command.temperature() : 0.3);
                    if (command.systemPrompt() != null && !command.systemPrompt().isBlank()) {
                        payload.put("system", command.systemPrompt().trim());
                    }
                    ArrayNode messages = mapper.createArrayNode();
                    adapter.normalizeMessages(command).forEach(message -> messages.add(mapper.createObjectNode()
                            .put("role", "assistant".equalsIgnoreCase(message.role()) ? "assistant" : "user")
                            .set("content", mapper.createArrayNode().add(
                                    mapper.createObjectNode()
                                            .put("type", "text")
                                            .put("text", message.content())
                            ))));
                    payload.set("messages", messages);
                    return payload;
                },
                response -> {
                    StringBuilder content = new StringBuilder();
                    response.path("content").forEach(item -> {
                        if ("text".equalsIgnoreCase(item.path("type").asText())) {
                            content.append(item.path("text").asText(""));
                        }
                    });
                    return content.toString();
                }
        );
    }

    @Override
    protected String operationPath(ModelDefinition model) {
        return baseUrl() + "/messages";
    }
}
