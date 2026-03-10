package com.lume.workspace.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lume.workspace.dto.AiPlatformModels;
import com.lume.workspace.inference.ModelDefinition;
import com.lume.workspace.inference.ProviderDefinition;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

@Service
public class LanguageCapabilityService {

    private static final Map<String, String> DEFAULT_TRANSLATION_MODELS = Map.of(
            "google-translation", "google-translation:text"
    );
    private static final Map<String, String> DEFAULT_NLP_MODELS = Map.of(
            "google-natural-language", "google-natural-language:analyze-entities"
    );

    private final ProviderCatalogService providerCatalogService;
    private final WorkspaceContextService workspaceContextService;
    private final AuditLogService auditLogService;
    private final GoogleCloudSupportService googleCloudSupportService;
    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

    public LanguageCapabilityService(
            ProviderCatalogService providerCatalogService,
            WorkspaceContextService workspaceContextService,
            AuditLogService auditLogService,
            GoogleCloudSupportService googleCloudSupportService,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper
    ) {
        this.providerCatalogService = providerCatalogService;
        this.workspaceContextService = workspaceContextService;
        this.auditLogService = auditLogService;
        this.googleCloudSupportService = googleCloudSupportService;
        this.restClientBuilder = restClientBuilder;
        this.objectMapper = objectMapper;
    }

    public AiPlatformModels.TranslationResponse translate(AiPlatformModels.TranslationRequest request) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_WORKSPACE_READ);
        ProviderDefinition provider = providerCatalogService.requireProvider(request.providerCode());
        if (!"google-translation".equals(provider.code())) {
            return new AiPlatformModels.TranslationResponse(provider.code(), provider.name(), request.modelCode(), "unsupported", null, null, "O provedor selecionado nao oferece traducao nesta integracao.");
        }
        List<String> missingCredentials = providerCatalogService.missingCredentialEnvVars(provider);
        if (!missingCredentials.isEmpty()) {
            return new AiPlatformModels.TranslationResponse(provider.code(), provider.name(), resolveModel(provider, request.modelCode(), DEFAULT_TRANSLATION_MODELS).code(), "missing_credentials", null, null, "Credenciais ausentes: " + String.join(", ", missingCredentials));
        }
        try {
            GoogleCloudSupportService.GoogleAccessContext accessContext = googleCloudSupportService.accessContext(provider);
            ModelDefinition model = resolveModel(provider, request.modelCode(), DEFAULT_TRANSLATION_MODELS);
            ObjectNode payload = objectMapper.createObjectNode();
            payload.putArray("contents").add(request.text());
            payload.put("targetLanguageCode", request.targetLanguageCode().trim());
            if (request.sourceLanguageCode() != null && !request.sourceLanguageCode().isBlank()) {
                payload.put("sourceLanguageCode", request.sourceLanguageCode().trim());
            }
            JsonNode response = restClientBuilder.build()
                    .post()
                    .uri(providerCatalogService.resolveBaseUrl(provider) + "/projects/" + accessContext.projectId() + "/locations/global:translateText")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + accessContext.accessToken())
                    .body(payload)
                    .retrieve()
                    .body(JsonNode.class);

            JsonNode translation = response.path("translations").isArray() && !response.path("translations").isEmpty()
                    ? response.path("translations").get(0)
                    : objectMapper.nullNode();
            AiPlatformModels.TranslationResponse result = new AiPlatformModels.TranslationResponse(
                    provider.code(),
                    provider.name(),
                    model.code(),
                    "completed",
                    translation.path("translatedText").asText(""),
                    translation.path("detectedLanguageCode").asText(null),
                    null
            );
            auditLogService.record("ai_translation_text", provider.code(), result.status(), Map.of(
                    "providerCode", provider.code(),
                    "modelCode", result.modelCode(),
                    "status", result.status()
            ));
            return result;
        } catch (RestClientException providerError) {
            return new AiPlatformModels.TranslationResponse(provider.code(), provider.name(), resolveModel(provider, request.modelCode(), DEFAULT_TRANSLATION_MODELS).code(), "provider_error", null, null, providerError.getMessage());
        }
    }

    public AiPlatformModels.NlpAnalysisResponse analyze(AiPlatformModels.NlpAnalysisRequest request) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_WORKSPACE_READ);
        ProviderDefinition provider = providerCatalogService.requireProvider(request.providerCode());
        if (!"google-natural-language".equals(provider.code())) {
            return new AiPlatformModels.NlpAnalysisResponse(provider.code(), provider.name(), request.modelCode(), "unsupported", request.languageCode(), null, null, List.of(), List.of(), "O provedor selecionado nao oferece NLP nesta integracao.");
        }
        List<String> missingCredentials = providerCatalogService.missingCredentialEnvVars(provider);
        if (!missingCredentials.isEmpty()) {
            return new AiPlatformModels.NlpAnalysisResponse(provider.code(), provider.name(), resolveModel(provider, request.modelCode(), DEFAULT_NLP_MODELS).code(), "missing_credentials", request.languageCode(), null, null, List.of(), List.of(), "Credenciais ausentes: " + String.join(", ", missingCredentials));
        }
        try {
            GoogleCloudSupportService.GoogleAccessContext accessContext = googleCloudSupportService.accessContext(provider);
            ModelDefinition model = resolveModel(provider, request.modelCode(), DEFAULT_NLP_MODELS);
            String endpoint = switch (normalizeAnalysisType(request.analysisType())) {
                case "sentiment" -> "/documents:analyzeSentiment";
                case "classification" -> "/documents:classifyText";
                default -> "/documents:analyzeEntities";
            };
            ObjectNode payload = objectMapper.createObjectNode();
            ObjectNode document = payload.putObject("document");
            document.put("type", "PLAIN_TEXT");
            document.put("content", request.text());
            if (request.languageCode() != null && !request.languageCode().isBlank()) {
                document.put("language", request.languageCode().trim());
            }

            JsonNode response = restClientBuilder.build()
                    .post()
                    .uri(providerCatalogService.resolveBaseUrl(provider) + endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + accessContext.accessToken())
                    .body(payload)
                    .retrieve()
                    .body(JsonNode.class);

            AiPlatformModels.NlpAnalysisResponse result = new AiPlatformModels.NlpAnalysisResponse(
                    provider.code(),
                    provider.name(),
                    model.code(),
                    "completed",
                    response.path("language").asText(request.languageCode()),
                    response.path("documentSentiment").path("score").isNumber() ? response.path("documentSentiment").path("score").asDouble() : null,
                    response.path("documentSentiment").path("magnitude").isNumber() ? response.path("documentSentiment").path("magnitude").asDouble() : null,
                    parseEntities(response),
                    parseCategories(response),
                    null
            );
            auditLogService.record("ai_nlp_analyze", provider.code(), result.status(), Map.of(
                    "providerCode", provider.code(),
                    "modelCode", result.modelCode(),
                    "status", result.status(),
                    "analysisType", normalizeAnalysisType(request.analysisType())
            ));
            return result;
        } catch (RestClientException providerError) {
            return new AiPlatformModels.NlpAnalysisResponse(provider.code(), provider.name(), resolveModel(provider, request.modelCode(), DEFAULT_NLP_MODELS).code(), "provider_error", request.languageCode(), null, null, List.of(), List.of(), providerError.getMessage());
        }
    }

    private String normalizeAnalysisType(String analysisType) {
        if (analysisType == null || analysisType.isBlank()) {
            return "entities";
        }
        return switch (analysisType.trim().toLowerCase()) {
            case "sentiment", "classification", "entities" -> analysisType.trim().toLowerCase();
            default -> "entities";
        };
    }

    private List<AiPlatformModels.NlpEntity> parseEntities(JsonNode response) {
        if (!response.path("entities").isArray()) {
            return List.of();
        }
        return java.util.stream.StreamSupport.stream(response.path("entities").spliterator(), false)
                .map(node -> new AiPlatformModels.NlpEntity(
                        node.path("name").asText(""),
                        node.path("type").asText(""),
                        node.path("salience").isNumber() ? node.path("salience").asDouble() : null
                ))
                .toList();
    }

    private List<AiPlatformModels.NlpCategory> parseCategories(JsonNode response) {
        if (!response.path("categories").isArray()) {
            return List.of();
        }
        return java.util.stream.StreamSupport.stream(response.path("categories").spliterator(), false)
                .map(node -> new AiPlatformModels.NlpCategory(
                        node.path("name").asText(""),
                        node.path("confidence").isNumber() ? node.path("confidence").asDouble() : null
                ))
                .toList();
    }

    private ModelDefinition resolveModel(ProviderDefinition provider, String requestedModelCode, Map<String, String> defaults) {
        if (requestedModelCode != null && !requestedModelCode.isBlank()) {
            return providerCatalogService.resolveModel(provider.code(), requestedModelCode);
        }
        return providerCatalogService.resolveModel(provider.code(), defaults.get(provider.code()));
    }
}
