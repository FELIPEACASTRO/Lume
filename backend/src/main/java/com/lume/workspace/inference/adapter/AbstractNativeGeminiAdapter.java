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

public abstract class AbstractNativeGeminiAdapter extends AbstractStrategyBackedAdapter {

    protected AbstractNativeGeminiAdapter(
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
                (provider, catalogService) -> Map.of("x-goog-api-key", catalogService.credentialValue(provider, "apiKey")),
                (command, model, adapter, mapper) -> {
                    ObjectNode payload = mapper.createObjectNode();
                    if (command.systemPrompt() != null && !command.systemPrompt().isBlank()) {
                        ObjectNode instruction = mapper.createObjectNode();
                        instruction.set("parts", mapper.createArrayNode().add(
                                mapper.createObjectNode().put("text", command.systemPrompt().trim())
                        ));
                        payload.set("system_instruction", instruction);
                    }
                    ArrayNode contents = mapper.createArrayNode();
                    adapter.normalizeMessages(command).forEach(message -> {
                        ObjectNode content = mapper.createObjectNode();
                        content.put("role", "assistant".equalsIgnoreCase(message.role()) ? "model" : "user");
                        content.set("parts", mapper.createArrayNode().add(
                                mapper.createObjectNode().put("text", message.content())
                        ));
                        contents.add(content);
                    });
                    payload.set("contents", contents);

                    ObjectNode generationConfig = mapper.createObjectNode();
                    generationConfig.put("temperature", command.temperature() != null ? command.temperature() : 0.3);
                    generationConfig.put("maxOutputTokens", command.maxTokens() != null ? command.maxTokens() : 700);
                    payload.set("generationConfig", generationConfig);
                    return payload;
                },
                response -> {
                    StringBuilder content = new StringBuilder();
                    response.path("candidates").forEach(candidate -> candidate.path("content").path("parts")
                            .forEach(part -> content.append(part.path("text").asText(""))));
                    return content.toString();
                }
        );
    }

    @Override
    protected String operationPath(ModelDefinition model) {
        return baseUrl() + "/models/" + externalModelCode(model.code()) + ":generateContent";
    }
}
