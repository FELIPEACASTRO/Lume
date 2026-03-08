package com.lume.workspace.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lume.workspace.dto.ResearchQueryRequest;
import com.lume.workspace.dto.ResearchQueryResponse;
import com.lume.workspace.dto.ResearchResultItemResponse;
import com.lume.workspace.inference.ProviderDefinition;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Service
public class ResearchService {

    private final ProviderCatalogService providerCatalogService;
    private final WorkspaceContextService workspaceContextService;
    private final AuditLogService auditLogService;
    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

    public ResearchService(
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

    public ResearchQueryResponse query(ResearchQueryRequest request) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_RESEARCH_RUN);

        ProviderDefinition provider = providerCatalogService.requireProvider(request.providerCode());
        if (!"research-search".equalsIgnoreCase(provider.category())) {
            return unsupported(provider, request.query(), "O provedor selecionado nao pertence a categoria de research.");
        }

        if (!provider.executionSupported()) {
            return unsupported(provider, request.query(), "O provedor de research segue catalogado/manual nesta rodada.");
        }

        List<String> missingCredentials = providerCatalogService.missingCredentialEnvVars(provider);
        if (!missingCredentials.isEmpty()) {
            return new ResearchQueryResponse(provider.code(), provider.name(), false, true, "missing_credentials", request.query(), List.of(), "Credenciais ausentes: " + String.join(", ", missingCredentials));
        }

        ResearchQueryResponse response;
        try {
            response = switch (provider.code()) {
                case "exa" -> queryExa(provider, request);
                case "newscatcher" -> queryNewsCatcher(provider, request);
                default -> unsupported(provider, request.query(), "O adapter deste provedor ainda nao foi implementado.");
            };
        } catch (RestClientException providerError) {
            response = new ResearchQueryResponse(provider.code(), provider.name(), true, provider.executionSupported(), "provider_error", request.query(), List.of(), providerError.getMessage());
        }

        auditLogService.record(
                "research_query",
                provider.code(),
                "executed",
                java.util.Map.of(
                        "providerCode", provider.code(),
                        "status", response.status(),
                        "query", request.query()
                )
        );
        return response;
    }

    private ResearchQueryResponse queryExa(ProviderDefinition provider, ResearchQueryRequest request) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("query", request.query());
        payload.put("numResults", request.limit() != null ? request.limit() : 5);

        JsonNode response = restClientBuilder.build()
                .post()
                .uri(providerCatalogService.resolveBaseUrl(provider) + "/search")
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-api-key", providerCatalogService.credentialValue(provider, "apiKey"))
                .body(payload)
                .retrieve()
                .body(JsonNode.class);

        List<ResearchResultItemResponse> items = response.path("results").isArray()
                ? java.util.stream.StreamSupport.stream(response.path("results").spliterator(), false)
                .map(result -> new ResearchResultItemResponse(
                        result.path("title").asText(""),
                        result.path("url").asText(""),
                        result.path("text").asText(""),
                        provider.name(),
                        result.has("score") ? result.path("score").asDouble() : null
                ))
                .toList()
                : List.of();

        return new ResearchQueryResponse(provider.code(), provider.name(), true, true, "completed", request.query(), items, null);
    }

    private ResearchQueryResponse queryNewsCatcher(ProviderDefinition provider, ResearchQueryRequest request) {
        String uri = UriComponentsBuilder.fromHttpUrl(providerCatalogService.resolveBaseUrl(provider) + "/search")
                .queryParam("q", request.query())
                .queryParam("page_size", request.limit() != null ? request.limit() : 5)
                .toUriString();

        JsonNode response = restClientBuilder.build()
                .get()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON)
                .header("x-api-token", providerCatalogService.credentialValue(provider, "apiToken"))
                .retrieve()
                .body(JsonNode.class);

        List<ResearchResultItemResponse> items = response.path("articles").isArray()
                ? java.util.stream.StreamSupport.stream(response.path("articles").spliterator(), false)
                .map(article -> new ResearchResultItemResponse(
                        article.path("title").asText(""),
                        article.path("link").asText(article.path("url").asText("")),
                        article.path("summary").asText(article.path("excerpt").asText("")),
                        provider.name(),
                        article.has("rank") ? article.path("rank").asDouble() : null
                ))
                .toList()
                : List.of();

        return new ResearchQueryResponse(provider.code(), provider.name(), true, true, "completed", request.query(), items, null);
    }

    private ResearchQueryResponse unsupported(ProviderDefinition provider, String query, String error) {
        return new ResearchQueryResponse(provider.code(), provider.name(), providerCatalogService.isConfigured(provider), provider.executionSupported(), "unsupported", query, List.of(), error);
    }
}
