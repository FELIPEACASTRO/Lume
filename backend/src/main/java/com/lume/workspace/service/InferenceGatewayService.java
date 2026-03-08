package com.lume.workspace.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lume.workspace.dto.UnifiedInferenceRequest;
import com.lume.workspace.dto.UnifiedInferenceResponse;
import com.lume.workspace.dto.UnifiedMessageRequest;
import com.lume.workspace.inference.ModelDefinition;
import com.lume.workspace.inference.ProviderDefinition;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;

@Service
public class InferenceGatewayService {

    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;
    private final ProviderCatalogService providerCatalogService;

    public InferenceGatewayService(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            ProviderCatalogService providerCatalogService,
            org.springframework.core.env.Environment environment
    ) {
        this.restClientBuilder = restClientBuilder;
        this.objectMapper = objectMapper;
        this.providerCatalogService = providerCatalogService;
    }

    public UnifiedInferenceResponse execute(UnifiedInferenceRequest request) {
        ProviderDefinition provider = providerCatalogService.requireProvider(request.providerCode());
        ModelDefinition model = providerCatalogService.resolveModel(provider.code(), request.modelCode());

        if (!provider.executionSupported()) {
            return unavailable(provider, model, "unsupported", "Este provedor esta catalogado, mas nao foi habilitado para execucao real nesta rodada.");
        }

        List<String> missingCredentials = providerCatalogService.missingCredentialEnvVars(provider);
        if (!missingCredentials.isEmpty()) {
            return unavailable(provider, model, "missing_credentials", "Credenciais ausentes para " + provider.name() + ": " + String.join(", ", missingCredentials));
        }

        try {
            return switch (provider.protocol()) {
                case OPENAI_RESPONSES -> callOpenAiResponses(provider, model, request);
                case OPENAI_CHAT_COMPLETIONS, CLOUDFLARE_OPENAI_COMPAT -> callOpenAiCompatible(provider, model, request);
                case ANTHROPIC_MESSAGES -> callAnthropic(provider, model, request);
                case GEMINI_GENERATE_CONTENT -> callGemini(provider, model, request);
                case COHERE_CHAT_V2 -> callCohere(provider, model, request);
                case BEDROCK_CONVERSE, CUSTOM_RESEARCH, CUSTOM_THREAT_INTEL, UNSUPPORTED ->
                        unavailable(provider, model, "unsupported", "O protocolo deste provedor ainda nao foi implementado para inferencia textual.");
            };
        } catch (RestClientException providerError) {
            return unavailable(provider, model, "provider_error", providerError.getMessage());
        }
    }

    private UnifiedInferenceResponse callOpenAiResponses(
            ProviderDefinition provider,
            ModelDefinition model,
            UnifiedInferenceRequest request
    ) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", externalModelCode(model.code()));
        payload.put("temperature", request.temperature() != null ? request.temperature() : 0.3);
        payload.put("max_output_tokens", request.maxTokens() != null ? request.maxTokens() : 700);
        payload.put("input", stringifyMessages(request));

        JsonNode response = restClientBuilder.build()
                .post()
                .uri(providerCatalogService.resolveBaseUrl(provider) + "/responses")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + providerCatalogService.credentialValue(provider, "apiKey"))
                .body(payload)
                .retrieve()
                .body(JsonNode.class);

        return success(provider, model, extractResponsesContent(response));
    }

    private UnifiedInferenceResponse callOpenAiCompatible(
            ProviderDefinition provider,
            ModelDefinition model,
            UnifiedInferenceRequest request
    ) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", externalModelCode(model.code()));
        payload.put("temperature", request.temperature() != null ? request.temperature() : 0.3);
        payload.put("max_tokens", request.maxTokens() != null ? request.maxTokens() : 700);
        payload.set("messages", buildOpenAiMessages(request));

        RestClient.RequestBodySpec spec = restClientBuilder.build()
                .post()
                .uri(providerCatalogService.resolveBaseUrl(provider) + "/chat/completions")
                .contentType(MediaType.APPLICATION_JSON);

        if ("cloudflare-workers-ai".equals(provider.code())) {
            spec.header("Authorization", "Bearer " + providerCatalogService.credentialValue(provider, "apiToken"));
        } else {
            spec.header("Authorization", "Bearer " + providerCatalogService.credentialValue(provider, "apiKey"));
        }

        JsonNode response = spec.body(payload)
                .retrieve()
                .body(JsonNode.class);

        return success(provider, model, extractOpenAiContent(response));
    }

    private UnifiedInferenceResponse callAnthropic(
            ProviderDefinition provider,
            ModelDefinition model,
            UnifiedInferenceRequest request
    ) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", externalModelCode(model.code()));
        payload.put("max_tokens", request.maxTokens() != null ? request.maxTokens() : 700);
        payload.put("temperature", request.temperature() != null ? request.temperature() : 0.3);
        if (request.systemPrompt() != null && !request.systemPrompt().isBlank()) {
            payload.put("system", request.systemPrompt().trim());
        }
        payload.set("messages", buildAnthropicMessages(request));

        JsonNode response = restClientBuilder.build()
                .post()
                .uri(providerCatalogService.resolveBaseUrl(provider) + "/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-api-key", providerCatalogService.credentialValue(provider, "apiKey"))
                .header("anthropic-version", "2023-06-01")
                .body(payload)
                .retrieve()
                .body(JsonNode.class);

        StringBuilder content = new StringBuilder();
        if (response != null) {
            for (JsonNode item : response.path("content")) {
                if ("text".equalsIgnoreCase(item.path("type").asText())) {
                    content.append(item.path("text").asText(""));
                }
            }
        }

        return success(provider, model, content.toString());
    }

    private UnifiedInferenceResponse callGemini(
            ProviderDefinition provider,
            ModelDefinition model,
            UnifiedInferenceRequest request
    ) {
        ObjectNode payload = objectMapper.createObjectNode();

        if (request.systemPrompt() != null && !request.systemPrompt().isBlank()) {
            ObjectNode instruction = objectMapper.createObjectNode();
            instruction.set("parts", objectMapper.createArrayNode().add(
                    objectMapper.createObjectNode().put("text", request.systemPrompt().trim())
            ));
            payload.set("system_instruction", instruction);
        }

        ArrayNode contents = objectMapper.createArrayNode();
        for (UnifiedMessageRequest message : normalizeMessages(request)) {
            ObjectNode content = objectMapper.createObjectNode();
            content.put("role", "assistant".equalsIgnoreCase(message.role()) ? "model" : "user");
            content.set("parts", objectMapper.createArrayNode().add(
                    objectMapper.createObjectNode().put("text", message.content())
            ));
            contents.add(content);
        }
        payload.set("contents", contents);

        ObjectNode generationConfig = objectMapper.createObjectNode();
        generationConfig.put("temperature", request.temperature() != null ? request.temperature() : 0.3);
        generationConfig.put("maxOutputTokens", request.maxTokens() != null ? request.maxTokens() : 700);
        payload.set("generationConfig", generationConfig);

        JsonNode response = restClientBuilder.build()
                .post()
                .uri(providerCatalogService.resolveBaseUrl(provider) + "/models/" + externalModelCode(model.code()) + ":generateContent")
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-goog-api-key", providerCatalogService.credentialValue(provider, "apiKey"))
                .body(payload)
                .retrieve()
                .body(JsonNode.class);

        StringBuilder content = new StringBuilder();
        if (response != null) {
            for (JsonNode candidate : response.path("candidates")) {
                for (JsonNode part : candidate.path("content").path("parts")) {
                    content.append(part.path("text").asText(""));
                }
            }
        }

        return success(provider, model, content.toString());
    }

    private UnifiedInferenceResponse callCohere(
            ProviderDefinition provider,
            ModelDefinition model,
            UnifiedInferenceRequest request
    ) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", externalModelCode(model.code()));
        payload.put("temperature", request.temperature() != null ? request.temperature() : 0.3);
        if (request.systemPrompt() != null && !request.systemPrompt().isBlank()) {
            payload.put("preamble", request.systemPrompt().trim());
        }
        payload.put("message", stringifyMessages(request));

        JsonNode response = restClientBuilder.build()
                .post()
                .uri(providerCatalogService.resolveBaseUrl(provider) + "/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + providerCatalogService.credentialValue(provider, "apiKey"))
                .body(payload)
                .retrieve()
                .body(JsonNode.class);

        return success(provider, model, response.path("message").path("content").path(0).path("text").asText(""));
    }

    private ArrayNode buildOpenAiMessages(UnifiedInferenceRequest request) {
        ArrayNode messages = objectMapper.createArrayNode();

        if (request.systemPrompt() != null && !request.systemPrompt().isBlank()) {
            messages.add(objectMapper.createObjectNode()
                    .put("role", "system")
                    .put("content", request.systemPrompt().trim()));
        }

        for (UnifiedMessageRequest message : normalizeMessages(request)) {
            messages.add(objectMapper.createObjectNode()
                    .put("role", normalizeRole(message.role()))
                    .put("content", message.content()));
        }

        return messages;
    }

    private ArrayNode buildAnthropicMessages(UnifiedInferenceRequest request) {
        ArrayNode messages = objectMapper.createArrayNode();
        for (UnifiedMessageRequest message : normalizeMessages(request)) {
            messages.add(objectMapper.createObjectNode()
                    .put("role", "assistant".equalsIgnoreCase(message.role()) ? "assistant" : "user")
                    .set("content", objectMapper.createArrayNode().add(
                            objectMapper.createObjectNode()
                                    .put("type", "text")
                                    .put("text", message.content())
                    )));
        }
        return messages;
    }

    private List<UnifiedMessageRequest> normalizeMessages(UnifiedInferenceRequest request) {
        if (request.messages() != null && !request.messages().isEmpty()) {
            return request.messages().stream()
                    .filter(message -> message != null && message.content() != null && !message.content().isBlank())
                    .toList();
        }

        List<UnifiedMessageRequest> fallbackMessages = new ArrayList<>();
        if (request.prompt() != null && !request.prompt().isBlank()) {
            fallbackMessages.add(new UnifiedMessageRequest("user", request.prompt().trim()));
        }
        return fallbackMessages;
    }

    private String extractResponsesContent(JsonNode response) {
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

    private String extractOpenAiContent(JsonNode response) {
        JsonNode contentNode = response.path("choices").path(0).path("message").path("content");
        if (contentNode.isTextual()) {
            return contentNode.asText("");
        }

        if (contentNode.isArray()) {
            StringBuilder content = new StringBuilder();
            for (JsonNode node : contentNode) {
                if (node.isTextual()) {
                    content.append(node.asText());
                } else {
                    content.append(node.path("text").asText(""));
                }
            }
            return content.toString();
        }

        return "";
    }

    private String stringifyMessages(UnifiedInferenceRequest request) {
        StringBuilder builder = new StringBuilder();
        if (request.systemPrompt() != null && !request.systemPrompt().isBlank()) {
            builder.append("System: ").append(request.systemPrompt().trim()).append("\n\n");
        }
        for (UnifiedMessageRequest message : normalizeMessages(request)) {
            builder.append(normalizeRole(message.role())).append(": ").append(message.content()).append("\n");
        }
        return builder.toString().trim();
    }

    private String externalModelCode(String internalModelCode) {
        int separator = internalModelCode.indexOf(':');
        return separator > -1 ? internalModelCode.substring(separator + 1) : internalModelCode;
    }

    private String normalizeRole(String role) {
        if ("assistant".equalsIgnoreCase(role)) {
            return "assistant";
        }
        if ("system".equalsIgnoreCase(role)) {
            return "system";
        }
        return "user";
    }

    private UnifiedInferenceResponse success(ProviderDefinition provider, ModelDefinition model, String content) {
        return new UnifiedInferenceResponse(
                provider.code(),
                provider.name(),
                model.code(),
                model.versionLabel(),
                provider.apiStyle(),
                true,
                true,
                false,
                "completed",
                content == null ? "" : content.trim(),
                null
        );
    }

    private UnifiedInferenceResponse unavailable(ProviderDefinition provider, ModelDefinition model, String status, String error) {
        return new UnifiedInferenceResponse(
                provider.code(),
                provider.name(),
                model.code(),
                model.versionLabel(),
                provider.apiStyle(),
                providerCatalogService.isConfigured(provider),
                provider.executionSupported(),
                false,
                status,
                null,
                error
        );
    }
}
