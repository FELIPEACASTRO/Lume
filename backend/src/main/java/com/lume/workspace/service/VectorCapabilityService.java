package com.lume.workspace.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lume.workspace.dto.AiPlatformModels;
import com.lume.workspace.inference.ModelDefinition;
import com.lume.workspace.inference.ProviderDefinition;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class VectorCapabilityService {

    private static final Map<String, String> DEFAULT_EMBEDDING_MODELS = Map.of(
            "cohere", "cohere:embed-v4.0",
            "voyage-ai", "voyage-ai:voyage-3-large"
    );

    private static final Map<String, String> DEFAULT_RERANK_MODELS = Map.of(
            "cohere", "cohere:rerank-v3.5",
            "voyage-ai", "voyage-ai:rerank-2"
    );

    private final ProviderCatalogService providerCatalogService;
    private final WorkspaceContextService workspaceContextService;
    private final AuditLogService auditLogService;
    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

    public VectorCapabilityService(
            ProviderCatalogService providerCatalogService,
            WorkspaceContextService workspaceContextService,
            AuditLogService auditLogService,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper
    ) {
        this.providerCatalogService = providerCatalogService;
        this.workspaceContextService = workspaceContextService;
        this.auditLogService = auditLogService;
        this.restClientBuilder = restClientBuilder;
        this.objectMapper = objectMapper;
    }

    public AiPlatformModels.EmbeddingResponse embeddings(AiPlatformModels.EmbeddingRequest request) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_WORKSPACE_READ);

        ProviderDefinition provider = providerCatalogService.requireProvider(request.providerCode());
        if (!supportsEmbeddings(provider)) {
            return new AiPlatformModels.EmbeddingResponse(
                    provider.code(),
                    provider.name(),
                    request.modelCode(),
                    "unsupported",
                    List.of(),
                    null,
                    null,
                    "O provedor selecionado nao oferece embeddings nesta integracao."
            );
        }

        List<String> missingCredentials = providerCatalogService.missingCredentialEnvVars(provider);
        if (!missingCredentials.isEmpty()) {
            return new AiPlatformModels.EmbeddingResponse(
                    provider.code(),
                    provider.name(),
                    resolveEmbeddingModel(provider, request.modelCode()).code(),
                    "missing_credentials",
                    List.of(),
                    null,
                    null,
                    "Credenciais ausentes: " + String.join(", ", missingCredentials)
            );
        }

        try {
            AiPlatformModels.EmbeddingResponse response = switch (provider.code()) {
                case "cohere" -> cohereEmbeddings(provider, request);
                case "voyage-ai" -> voyageEmbeddings(provider, request);
                default -> throw new IllegalStateException("Provider de embeddings nao suportado: " + provider.code());
            };
            auditLogService.record("ai_embeddings", provider.code(), response.status(), Map.of(
                    "providerCode", provider.code(),
                    "modelCode", response.modelCode(),
                    "status", response.status()
            ));
            return response;
        } catch (RestClientException providerError) {
            return new AiPlatformModels.EmbeddingResponse(
                    provider.code(),
                    provider.name(),
                    resolveEmbeddingModel(provider, request.modelCode()).code(),
                    "provider_error",
                    List.of(),
                    null,
                    null,
                    providerError.getMessage()
            );
        }
    }

    public AiPlatformModels.RerankResponse rerank(AiPlatformModels.RerankRequest request) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_WORKSPACE_READ);

        ProviderDefinition provider = providerCatalogService.requireProvider(request.providerCode());
        if (!supportsRerank(provider)) {
            return new AiPlatformModels.RerankResponse(
                    provider.code(),
                    provider.name(),
                    request.modelCode(),
                    "unsupported",
                    List.of(),
                    List.of(),
                    null,
                    "O provedor selecionado nao oferece reranqueamento nesta integracao."
            );
        }

        List<String> missingCredentials = providerCatalogService.missingCredentialEnvVars(provider);
        if (!missingCredentials.isEmpty()) {
            return new AiPlatformModels.RerankResponse(
                    provider.code(),
                    provider.name(),
                    resolveRerankModel(provider, request.modelCode()).code(),
                    "missing_credentials",
                    List.of(),
                    List.of(),
                    null,
                    "Credenciais ausentes: " + String.join(", ", missingCredentials)
            );
        }

        try {
            AiPlatformModels.RerankResponse response = switch (provider.code()) {
                case "cohere" -> cohereRerank(provider, request);
                case "voyage-ai" -> voyageRerank(provider, request);
                default -> throw new IllegalStateException("Provider de rerank nao suportado: " + provider.code());
            };
            auditLogService.record("ai_rerank", provider.code(), response.status(), Map.of(
                    "providerCode", provider.code(),
                    "modelCode", response.modelCode(),
                    "status", response.status(),
                    "documents", request.documents() == null ? 0 : request.documents().size()
            ));
            return response;
        } catch (RestClientException providerError) {
            return new AiPlatformModels.RerankResponse(
                    provider.code(),
                    provider.name(),
                    resolveRerankModel(provider, request.modelCode()).code(),
                    "provider_error",
                    List.of(),
                    List.of(),
                    null,
                    providerError.getMessage()
            );
        }
    }

    private AiPlatformModels.EmbeddingResponse cohereEmbeddings(ProviderDefinition provider, AiPlatformModels.EmbeddingRequest request) {
        ModelDefinition model = resolveEmbeddingModel(provider, request.modelCode());
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", externalModelCode(model.code()));
        payload.put("input_type", "search_document");
        ArrayNode texts = objectMapper.createArrayNode();
        texts.add(request.input());
        payload.set("texts", texts);
        payload.set("embedding_types", objectMapper.createArrayNode().add("float"));

        JsonNode response = restClientBuilder.build()
                .post()
                .uri(providerCatalogService.resolveBaseUrl(provider) + "/embed")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + providerCatalogService.credentialValue(provider, "apiKey"))
                .body(payload)
                .retrieve()
                .body(JsonNode.class);

        List<List<Double>> embeddings = parseEmbeddings(response.path("embeddings").path("float"), response.path("embeddings"));
        return new AiPlatformModels.EmbeddingResponse(
                provider.code(),
                provider.name(),
                model.code(),
                "completed",
                embeddings,
                embeddings.isEmpty() ? null : embeddings.getFirst().size(),
                usageFromTokens(response.path("meta").path("billed_units").path("input_tokens").isNumber()
                        ? response.path("meta").path("billed_units").path("input_tokens").asInt()
                        : response.path("usage").path("input_tokens").isNumber()
                        ? response.path("usage").path("input_tokens").asInt()
                        : null),
                null
        );
    }

    private AiPlatformModels.EmbeddingResponse voyageEmbeddings(ProviderDefinition provider, AiPlatformModels.EmbeddingRequest request) {
        ModelDefinition model = resolveEmbeddingModel(provider, request.modelCode());
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", externalModelCode(model.code()));
        payload.put("input_type", "document");
        ArrayNode input = objectMapper.createArrayNode();
        input.add(request.input());
        payload.set("input", input);

        JsonNode response = restClientBuilder.build()
                .post()
                .uri(providerCatalogService.resolveBaseUrl(provider) + "/embeddings")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + providerCatalogService.credentialValue(provider, "apiKey"))
                .body(payload)
                .retrieve()
                .body(JsonNode.class);

        List<List<Double>> embeddings = response.path("data").isArray()
                ? java.util.stream.StreamSupport.stream(response.path("data").spliterator(), false)
                .map(node -> parseVector(node.path("embedding")))
                .toList()
                : List.of();

        Integer totalTokens = response.path("usage").path("total_tokens").isNumber()
                ? response.path("usage").path("total_tokens").asInt()
                : response.path("usage").path("input_tokens").isNumber()
                ? response.path("usage").path("input_tokens").asInt()
                : null;
        return new AiPlatformModels.EmbeddingResponse(
                provider.code(),
                provider.name(),
                model.code(),
                "completed",
                embeddings,
                embeddings.isEmpty() ? null : embeddings.getFirst().size(),
                usageFromTokens(totalTokens),
                null
        );
    }

    private AiPlatformModels.RerankResponse cohereRerank(ProviderDefinition provider, AiPlatformModels.RerankRequest request) {
        ModelDefinition model = resolveRerankModel(provider, request.modelCode());
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", externalModelCode(model.code()));
        payload.put("query", request.query());
        payload.put("top_n", request.topN() != null ? request.topN() : request.documents() == null ? 0 : request.documents().size());
        ArrayNode documents = objectMapper.createArrayNode();
        if (request.documents() != null) {
            request.documents().forEach(documents::add);
        }
        payload.set("documents", documents);

        JsonNode response = restClientBuilder.build()
                .post()
                .uri(providerCatalogService.resolveBaseUrl(provider) + "/rerank")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + providerCatalogService.credentialValue(provider, "apiKey"))
                .body(payload)
                .retrieve()
                .body(JsonNode.class);

        List<Integer> rankedIndexes = response.path("results").isArray()
                ? java.util.stream.StreamSupport.stream(response.path("results").spliterator(), false)
                .map(result -> result.path("index").asInt())
                .toList()
                : List.of();
        return new AiPlatformModels.RerankResponse(
                provider.code(),
                provider.name(),
                model.code(),
                "completed",
                rankedIndexes,
                rankedDocuments(rankedIndexes, request.documents()),
                usageFromTokens(response.path("meta").path("billed_units").path("search_units").isNumber()
                        ? response.path("meta").path("billed_units").path("search_units").asInt()
                        : response.path("usage").path("input_tokens").isNumber()
                        ? response.path("usage").path("input_tokens").asInt()
                        : null),
                null
        );
    }

    private AiPlatformModels.RerankResponse voyageRerank(ProviderDefinition provider, AiPlatformModels.RerankRequest request) {
        ModelDefinition model = resolveRerankModel(provider, request.modelCode());
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", externalModelCode(model.code()));
        payload.put("query", request.query());
        payload.put("top_k", request.topN() != null ? request.topN() : request.documents() == null ? 0 : request.documents().size());
        ArrayNode documents = objectMapper.createArrayNode();
        if (request.documents() != null) {
            request.documents().forEach(documents::add);
        }
        payload.set("documents", documents);

        JsonNode response = restClientBuilder.build()
                .post()
                .uri(providerCatalogService.resolveBaseUrl(provider) + "/rerank")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + providerCatalogService.credentialValue(provider, "apiKey"))
                .body(payload)
                .retrieve()
                .body(JsonNode.class);

        JsonNode results = response.path("data").isArray() ? response.path("data") : response.path("results");
        List<Integer> rankedIndexes = results.isArray()
                ? java.util.stream.StreamSupport.stream(results.spliterator(), false)
                .map(result -> result.path("index").asInt())
                .toList()
                : List.of();
        Integer totalTokens = response.path("usage").path("total_tokens").isNumber()
                ? response.path("usage").path("total_tokens").asInt()
                : null;
        return new AiPlatformModels.RerankResponse(
                provider.code(),
                provider.name(),
                model.code(),
                "completed",
                rankedIndexes,
                rankedDocuments(rankedIndexes, request.documents()),
                usageFromTokens(totalTokens),
                null
        );
    }

    private boolean supportsEmbeddings(ProviderDefinition provider) {
        return provider.capabilities().contains("embeddings") && (provider.code().equals("cohere") || provider.code().equals("voyage-ai"));
    }

    private boolean supportsRerank(ProviderDefinition provider) {
        return provider.capabilities().contains("rerank") && (provider.code().equals("cohere") || provider.code().equals("voyage-ai"));
    }

    private ModelDefinition resolveEmbeddingModel(ProviderDefinition provider, String requestedModelCode) {
        if (requestedModelCode != null && !requestedModelCode.isBlank()) {
            return providerCatalogService.resolveModel(provider.code(), requestedModelCode);
        }
        return providerCatalogService.resolveModel(provider.code(), DEFAULT_EMBEDDING_MODELS.get(provider.code()));
    }

    private ModelDefinition resolveRerankModel(ProviderDefinition provider, String requestedModelCode) {
        if (requestedModelCode != null && !requestedModelCode.isBlank()) {
            return providerCatalogService.resolveModel(provider.code(), requestedModelCode);
        }
        return providerCatalogService.resolveModel(provider.code(), DEFAULT_RERANK_MODELS.get(provider.code()));
    }

    private String externalModelCode(String internalModelCode) {
        int separator = internalModelCode.indexOf(':');
        return separator > -1 ? internalModelCode.substring(separator + 1) : internalModelCode;
    }

    private List<List<Double>> parseEmbeddings(JsonNode preferredNode, JsonNode fallbackNode) {
        if (preferredNode != null && preferredNode.isArray() && preferredNode.size() > 0 && preferredNode.get(0).isArray()) {
            return java.util.stream.StreamSupport.stream(preferredNode.spliterator(), false)
                    .map(this::parseVector)
                    .toList();
        }
        if (fallbackNode != null && fallbackNode.isArray() && fallbackNode.size() > 0 && fallbackNode.get(0).isArray()) {
            return java.util.stream.StreamSupport.stream(fallbackNode.spliterator(), false)
                    .map(this::parseVector)
                    .toList();
        }
        return List.of();
    }

    private List<Double> parseVector(JsonNode embeddingNode) {
        if (!embeddingNode.isArray()) {
            return List.of();
        }
        List<Double> values = new ArrayList<>(embeddingNode.size());
        embeddingNode.forEach(value -> values.add(value.asDouble()));
        return List.copyOf(values);
    }

    private List<String> rankedDocuments(List<Integer> indexes, List<String> documents) {
        if (documents == null || documents.isEmpty() || indexes.isEmpty()) {
            return List.of();
        }
        return indexes.stream()
                .filter(index -> index >= 0 && index < documents.size())
                .map(documents::get)
                .toList();
    }

    private AiPlatformModels.UsageMetadata usageFromTokens(Integer inputTokens) {
        if (inputTokens == null) {
            return null;
        }
        return new AiPlatformModels.UsageMetadata(inputTokens, null, inputTokens, null);
    }
}
