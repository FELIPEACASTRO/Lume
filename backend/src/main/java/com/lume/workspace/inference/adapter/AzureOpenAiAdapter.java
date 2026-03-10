package com.lume.workspace.inference.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lume.workspace.inference.ModelDefinition;
import com.lume.workspace.inference.ProviderKey;
import com.lume.workspace.inference.config.AiRuntimeProperties;
import com.lume.workspace.inference.orchestration.AiHttpExecutor;
import com.lume.workspace.service.ProviderCatalogService;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Component
public class AzureOpenAiAdapter extends AbstractStrategyBackedAdapter {

    public AzureOpenAiAdapter(
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
                (provider, catalogService) -> Map.of("api-key", catalogService.credentialValue(provider, "apiKey")),
                (command, model, adapter, mapper) -> {
                    ObjectNode payload = mapper.createObjectNode();
                    payload.put("temperature", command.temperature() != null ? command.temperature() : 0.3);
                    payload.put("max_tokens", command.maxTokens() != null ? command.maxTokens() : 700);
                    ArrayNode messages = mapper.createArrayNode();
                    if (command.systemPrompt() != null && !command.systemPrompt().isBlank()) {
                        messages.add(mapper.createObjectNode()
                                .put("role", "system")
                                .put("content", command.systemPrompt().trim()));
                    }
                    adapter.normalizeMessages(command).forEach(message -> messages.add(mapper.createObjectNode()
                            .put("role", adapter.normalizeRole(message.role()))
                            .put("content", message.content())));
                    payload.set("messages", messages);
                    return payload;
                },
                response -> {
                    JsonNode contentNode = response.path("choices").path(0).path("message").path("content");
                    return contentNode.isTextual() ? contentNode.asText("") : "";
                }
        );
    }

    @Override
    public ProviderKey providerKey() {
        return ProviderKey.AZURE_OPENAI;
    }

    @Override
    protected String operationPath(ModelDefinition model) {
        String deployment = providerCatalogService.credentialValue(provider(), "deployment");
        String apiVersion = providerCatalogService.credentialValue(provider(), "apiVersion");
        return UriComponentsBuilder
                .fromHttpUrl(baseUrl() + "/openai/deployments/" + deployment + "/chat/completions")
                .queryParam("api-version", apiVersion)
                .build()
                .toUriString();
    }
}
