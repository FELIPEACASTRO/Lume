package com.lume.workspace.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lume.infrastructure.config.SecurityComplianceProperties;
import com.lume.workspace.dto.ThreatIntelExposureDto;
import com.lume.workspace.dto.ThreatIntelQueryRequest;
import com.lume.workspace.dto.ThreatIntelQueryResponse;
import com.lume.workspace.inference.ProviderDefinition;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Base64;

@Service
public class ThreatIntelService {

    private final ProviderCatalogService providerCatalogService;
    private final WorkspaceContextService workspaceContextService;
    private final AuditLogService auditLogService;
    private final SecurityComplianceProperties securityComplianceProperties;
    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

    public ThreatIntelService(
            ProviderCatalogService providerCatalogService,
            WorkspaceContextService workspaceContextService,
            AuditLogService auditLogService,
            SecurityComplianceProperties securityComplianceProperties,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper
    ) {
        this.providerCatalogService = providerCatalogService;
        this.workspaceContextService = workspaceContextService;
        this.auditLogService = auditLogService;
        this.securityComplianceProperties = securityComplianceProperties;
        this.restClientBuilder = restClientBuilder;
        this.objectMapper = objectMapper;
    }

    public ThreatIntelQueryResponse query(ThreatIntelQueryRequest request) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_THREAT_INTEL_RUN);

        ProviderDefinition provider = providerCatalogService.requireProvider(request.providerCode());
        if (!"threat-intel".equalsIgnoreCase(provider.category())) {
            return unsupported(provider, request.query(), "O provedor selecionado nao pertence a categoria de threat-intel.");
        }

        if (!securityComplianceProperties.isDarkWebEnabled()) {
            ThreatIntelQueryResponse blocked = blocked(provider, request.query(), "A feature flag security.compliance.dark-web-enabled esta desligada.");
            auditLogService.record(
                    "threat_intel_query",
                    provider.code(),
                    "blocked",
                    Map.of(
                            "providerCode", provider.code(),
                            "status", blocked.status(),
                            "queryPreview", redactQuery(request.query()),
                            "justification", safeJustification(request.justification())
                    )
            );
            return blocked;
        }

        if (request.justification() == null || request.justification().isBlank()) {
            ThreatIntelQueryResponse invalid = blocked(provider, request.query(), "Uma justificativa e obrigatoria para consultas de threat-intel.");
            auditLogService.record(
                    "threat_intel_query",
                    provider.code(),
                    "blocked",
                    Map.of(
                            "providerCode", provider.code(),
                            "status", invalid.status(),
                            "queryPreview", redactQuery(request.query()),
                            "justification", safeJustification(request.justification())
                    )
            );
            return invalid;
        }

        List<String> missingCredentials = providerCatalogService.missingCredentialEnvVars(provider);
        if (!missingCredentials.isEmpty()) {
            ThreatIntelQueryResponse response = new ThreatIntelQueryResponse(
                    provider.code(),
                    provider.name(),
                    false,
                    provider.executionSupported(),
                    "missing_credentials",
                    request.query(),
                    List.of(),
                    "Credenciais ausentes: " + String.join(", ", missingCredentials)
            );
            audit(provider, request, response);
            return response;
        }

        ThreatIntelQueryResponse response;
        try {
            response = switch (provider.code()) {
                case "fullhunt" -> fullHuntQuery(provider, request);
                case "flare" -> flareQuery(provider, request);
                case "darkowl" -> darkOwlQuery(provider, request);
                case "onion-search-engine" -> onionSearchQuery(provider, request);
                case "twingly" -> twinglyQuery(provider, request);
                case "darknetsearch" -> darknetSearchQuery(provider, request);
                default -> unsupported(provider, request.query(), "Esta integracao ainda exige habilitacao manual antes do uso operacional.");
            };
        } catch (RestClientException providerError) {
            response = new ThreatIntelQueryResponse(
                    provider.code(),
                    provider.name(),
                    providerCatalogService.isConfigured(provider),
                    provider.executionSupported(),
                    "provider_error",
                    request.query(),
                    List.of(),
                    providerError.getMessage()
            );
        } catch (Exception unexpectedError) {
            response = new ThreatIntelQueryResponse(
                    provider.code(),
                    provider.name(),
                    providerCatalogService.isConfigured(provider),
                    provider.executionSupported(),
                    "provider_error",
                    request.query(),
                    List.of(),
                    unexpectedError.getMessage()
            );
        }

        audit(provider, request, response);
        return response;
    }

    private ThreatIntelQueryResponse fullHuntQuery(ProviderDefinition provider, ThreatIntelQueryRequest request) {
        int limit = normalizeLimit(request.limit());
        String url = UriComponentsBuilder.fromHttpUrl(providerCatalogService.resolveBaseUrl(provider))
                .path("/enterprise/darkweb/compromised-credentials")
                .queryParam("query", request.query())
                .queryParam("page", 1)
                .queryParam("size", limit)
                .toUriString();
        JsonNode response = restClientBuilder.build()
                .get()
                .uri(url)
                .header("x-api-key", providerCatalogService.credentialValue(provider, "apiKey"))
                .retrieve()
                .body(JsonNode.class);
        JsonNode results = response != null && response.path("results").isArray() ? response.path("results") : response;
        return completed(provider, request.query(), stream(results)
                .limit(limit)
                .map(item -> new ThreatIntelExposureDto(
                        firstNonBlank(item.path("email").asText(null), item.path("username").asText(null), item.path("domain").asText(null), "Exposicao encontrada"),
                        firstNonBlank(item.path("database_name").asText(null), "FullHunt Dark Web"),
                        item.path("password").isMissingNode() || item.path("password").asText("").isBlank() ? "media" : "alta",
                        firstNonBlank(item.path("database_name").asText(null), item.path("password").asText(null), item.path("source").asText(null)),
                        firstNonBlank(item.path("email").asText(null), item.path("domain").asText(null), item.path("source").asText(null))
                ))
                .toList());
    }

    private ThreatIntelQueryResponse flareQuery(ProviderDefinition provider, ThreatIntelQueryRequest request) {
        String token = flareAccessToken(provider);
        int limit = normalizeLimit(request.limit());
        JsonNode payload = objectMapper.createObjectNode()
                .put("size", limit)
                .set("query", objectMapper.createObjectNode()
                        .put("type", "query_string")
                        .put("query_string", request.query()));
        ((com.fasterxml.jackson.databind.node.ObjectNode) payload).set("filters", objectMapper.createObjectNode()
                .putArray("type")
                .add("chat_message")
                .add("forum_post"));

        JsonNode response = restClientBuilder.build()
                .post()
                .uri(providerCatalogService.resolveBaseUrl(provider) + "/firework/v4/events/global/_search")
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .body(payload)
                .retrieve()
                .body(JsonNode.class);

        JsonNode items = response != null && response.path("items").isArray() ? response.path("items") : response.path("results");
        return completed(provider, request.query(), stream(items)
                .limit(limit)
                .map(item -> new ThreatIntelExposureDto(
                        firstNonBlank(
                                item.path("title").asText(null),
                                item.path("metadata").path("title").asText(null),
                                item.path("metadata").path("uid").asText(null),
                                "Evento Flare"
                        ),
                        firstNonBlank(
                                item.path("metadata").path("source_type").asText(null),
                                item.path("type").asText(null),
                                "Flare"
                        ),
                        normalizeThreatSeverity(item.path("severity").asText(null)),
                        firstNonBlank(
                                item.path("rendered_message").asText(null),
                                item.path("body").asText(null),
                                item.path("content").asText(null),
                                item.path("metadata").path("description").asText(null)
                        ),
                        firstNonBlank(
                                item.path("metadata").path("uid").asText(null),
                                item.path("id").asText(null)
                        )
                ))
                .toList());
    }

    private ThreatIntelQueryResponse darkOwlQuery(ProviderDefinition provider, ThreatIntelQueryRequest request) throws Exception {
        int limit = normalizeLimit(request.limit());
        String dateHeader = DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC));
        String rawPath = "/api/v1/search?q=" + request.query() + "&detail=snippet&count=" + limit;
        String signature = darkOwlSignature(rawPath, dateHeader, providerCatalogService.credentialValue(provider, "privateKey"));
        String encodedUrl = UriComponentsBuilder.fromHttpUrl(providerCatalogService.resolveBaseUrl(provider))
                .path("/api/v1/search")
                .queryParam("q", request.query())
                .queryParam("detail", "snippet")
                .queryParam("count", limit)
                .toUriString();
        JsonNode response = restClientBuilder.build()
                .get()
                .uri(encodedUrl)
                .header("X-DarkOwl-Date", dateHeader)
                .header("X-DarkOwl-Authorization", providerCatalogService.credentialValue(provider, "publicKey") + ":" + signature)
                .retrieve()
                .body(JsonNode.class);

        JsonNode results = response != null && response.path("results").isArray() ? response.path("results") : response;
        return completed(provider, request.query(), stream(results)
                .limit(limit)
                .map(item -> new ThreatIntelExposureDto(
                        firstNonBlank(item.path("title").asText(null), item.path("url").asText(null), "Resultado DarkOwl"),
                        firstNonBlank(item.path("type").asText(null), "DarkOwl"),
                        normalizeThreatSeverity(item.path("hackishness").asText(null)),
                        firstNonBlank(item.path("snippet").asText(null), item.path("body").asText(null)),
                        firstNonBlank(item.path("url").asText(null), item.path("id").asText(null))
                ))
                .toList());
    }

    private ThreatIntelQueryResponse onionSearchQuery(ProviderDefinition provider, ThreatIntelQueryRequest request) {
        int limit = normalizeLimit(request.limit());
        String url = UriComponentsBuilder.fromHttpUrl(providerCatalogService.resolveBaseUrl(provider))
                .queryParam("q", request.query())
                .queryParam("limit", limit)
                .queryParam("type", "search")
                .toUriString();
        JsonNode response = restClientBuilder.build()
                .get()
                .uri(url)
                .header("x-api-key", providerCatalogService.credentialValue(provider, "apiKey"))
                .retrieve()
                .body(JsonNode.class);

        JsonNode items = extractCollection(response, "results", "items", "data");
        return completed(provider, request.query(), stream(items)
                .limit(limit)
                .map(item -> new ThreatIntelExposureDto(
                        firstNonBlank(
                                item.path("title").asText(null),
                                item.path("name").asText(null),
                                item.path("url").asText(null),
                                "Resultado Onion Search Engine"
                        ),
                        firstNonBlank(
                                item.path("domain").asText(null),
                                item.path("source").asText(null),
                                "Onion Search Engine"
                        ),
                        normalizeThreatSeverity(item.path("severity").asText(null)),
                        firstNonBlank(
                                item.path("snippet").asText(null),
                                item.path("description").asText(null),
                                item.path("text").asText(null)
                        ),
                        firstNonBlank(
                                item.path("url").asText(null),
                                item.path("link").asText(null),
                                item.path("id").asText(null)
                        )
                ))
                .toList());
    }

    private ThreatIntelQueryResponse twinglyQuery(ProviderDefinition provider, ThreatIntelQueryRequest request) {
        int limit = normalizeLimit(request.limit());
        JsonNode payload = objectMapper.createObjectNode()
                .put("query", request.query())
                .put("size", limit)
                .put("order", "desc")
                .put("sort", "crawled");
        JsonNode response = restClientBuilder.build()
                .post()
                .uri(providerCatalogService.resolveBaseUrl(provider))
                .header("Authorization", "apikey " + providerCatalogService.credentialValue(provider, "apiKey"))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Accept", "application/json; charset=utf-8")
                .body(payload)
                .retrieve()
                .body(JsonNode.class);

        JsonNode documents = extractCollection(response, "documents", "results", "items");
        return completed(provider, request.query(), stream(documents)
                .limit(limit)
                .map(item -> new ThreatIntelExposureDto(
                        firstNonBlank(
                                item.path("title").asText(null),
                                item.path("thread_title").asText(null),
                                item.path("uuid").asText(null),
                                "Resultado Twingly"
                        ),
                        firstNonBlank(
                                item.path("site_name").asText(null),
                                item.path("site_domain").asText(null),
                                item.path("site_type").asText(null),
                                "Twingly Dark Web"
                        ),
                        normalizeThreatSeverity(item.path("severity").asText(null)),
                        firstNonBlank(
                                item.path("text").asText(null),
                                item.path("excerpt").asText(null)
                        ),
                        firstNonBlank(
                                item.path("url").asText(null),
                                item.path("thread_url").asText(null),
                                item.path("uuid").asText(null)
                        )
                ))
                .toList());
    }

    private ThreatIntelQueryResponse darknetSearchQuery(ProviderDefinition provider, ThreatIntelQueryRequest request) {
        int limit = normalizeLimit(request.limit());
        String token = darknetSearchAccessToken(provider);
        String url = UriComponentsBuilder.fromHttpUrl(providerCatalogService.resolveBaseUrl(provider))
                .path("/service/leak_extended_database_search/")
                .queryParam("page", 0)
                .queryParam("size", limit)
                .queryParam("query", request.query())
                .queryParam("highlight", true)
                .queryParam("length", 300)
                .toUriString();
        JsonNode response = restClientBuilder.build()
                .get()
                .uri(url)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(JsonNode.class);

        JsonNode content = extractCollection(response, "content", "results", "items");
        return completed(provider, request.query(), stream(content)
                .limit(limit)
                .map(item -> new ThreatIntelExposureDto(
                        firstNonBlank(
                                item.path("fileName").asText(null),
                                item.path("leakId").asText(null),
                                item.path("website").asText(null),
                                "Resultado DarknetSearch"
                        ),
                        firstNonBlank(
                                item.path("website").asText(null),
                                item.path("source").asText(null),
                                "DarknetSearch"
                        ),
                        normalizeThreatSeverity(item.path("severity").asText(null)),
                        firstNonBlank(
                                item.path("highlightedContent").asText(null),
                                item.path("content").asText(null),
                                item.path("snippet").asText(null)
                        ),
                        firstNonBlank(
                                item.path("leakId").asText(null),
                                item.path("id").asText(null),
                                item.path("fileName").asText(null)
                        )
                ))
                .toList());
    }

    private String flareAccessToken(ProviderDefinition provider) {
        JsonNode response = restClientBuilder.build()
                .post()
                .uri(providerCatalogService.resolveBaseUrl(provider) + "/tokens/generate")
                .header("Authorization", providerCatalogService.credentialValue(provider, "apiKey"))
                .retrieve()
                .body(JsonNode.class);
        String token = firstNonBlank(
                response.path("token").asText(null),
                response.path("access_token").asText(null)
        );
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("O provider Flare nao retornou token de acesso temporario.");
        }
        return token;
    }

    private String darknetSearchAccessToken(ProviderDefinition provider) {
        String clientId = providerCatalogService.credentialValue(provider, "clientId");
        String clientSecret = providerCatalogService.credentialValue(provider, "clientSecret");
        String basic = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));
        String tokenUrl = UriComponentsBuilder.fromHttpUrl("https://app.leak.center/uaa/oauth/token")
                .queryParam("grant_type", "password")
                .queryParam("username", providerCatalogService.credentialValue(provider, "username"))
                .queryParam("password", providerCatalogService.credentialValue(provider, "password"))
                .queryParam("scope", "openid")
                .toUriString();
        JsonNode response = restClientBuilder.build()
                .post()
                .uri(tokenUrl)
                .header("Authorization", "Basic " + basic)
                .retrieve()
                .body(JsonNode.class);
        String token = firstNonBlank(
                response.path("access_token").asText(null),
                response.path("token").asText(null)
        );
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("O provider DarknetSearch nao retornou access_token.");
        }
        return token;
    }

    private String darkOwlSignature(String pathWithQuery, String dateHeader, String privateKey) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(privateKey.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
        byte[] digest = mac.doFinal((pathWithQuery + dateHeader).getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(digest);
    }

    private ThreatIntelQueryResponse completed(ProviderDefinition provider, String query, List<ThreatIntelExposureDto> items) {
        return new ThreatIntelQueryResponse(
                provider.code(),
                provider.name(),
                providerCatalogService.isConfigured(provider),
                provider.executionSupported(),
                "completed",
                query,
                items,
                null
        );
    }

    private java.util.stream.Stream<JsonNode> stream(JsonNode node) {
        if (node == null || !node.isArray()) {
            return java.util.stream.Stream.empty();
        }
        return java.util.stream.StreamSupport.stream(node.spliterator(), false);
    }

    private JsonNode extractCollection(JsonNode response, String... candidateFields) {
        if (response == null) {
            return objectMapper.createArrayNode();
        }
        if (response.isArray()) {
            return response;
        }
        for (String field : candidateFields) {
            JsonNode node = response.path(field);
            if (node.isArray()) {
                return node;
            }
        }
        return objectMapper.createArrayNode();
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return 5;
        }
        return Math.min(limit, 10);
    }

    private String normalizeThreatSeverity(String severity) {
        if (severity == null || severity.isBlank()) {
            return "media";
        }
        String normalized = severity.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "critical", "high", "alta", "severe" -> "alta";
            case "low", "baixa", "minor" -> "baixa";
            default -> "media";
        };
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private void audit(ProviderDefinition provider, ThreatIntelQueryRequest request, ThreatIntelQueryResponse response) {
        auditLogService.record(
                "threat_intel_query",
                provider.code(),
                response.status(),
                Map.of(
                        "providerCode", provider.code(),
                        "status", response.status(),
                        "queryPreview", redactQuery(request.query()),
                        "justification", safeJustification(request.justification())
                )
        );
    }

    private ThreatIntelQueryResponse blocked(ProviderDefinition provider, String query, String error) {
        return new ThreatIntelQueryResponse(provider.code(), provider.name(), providerCatalogService.isConfigured(provider), provider.executionSupported(), "compliance_blocked", query, List.of(), error);
    }

    private ThreatIntelQueryResponse unsupported(ProviderDefinition provider, String query, String error) {
        return new ThreatIntelQueryResponse(provider.code(), provider.name(), providerCatalogService.isConfigured(provider), provider.executionSupported(), "unsupported", query, List.of(), error);
    }

    private String redactQuery(String query) {
        if (query == null || query.isBlank()) {
            return "";
        }
        int visible = Math.min(6, query.length());
        return query.substring(0, visible) + "***";
    }

    private String safeJustification(String justification) {
        if (justification == null || justification.isBlank()) {
            return "missing";
        }
        String normalized = justification.trim().replaceAll("\\s+", " ");
        int previewLength = Math.min(24, normalized.length());
        return normalized.substring(0, previewLength) + (normalized.length() > previewLength ? "***" : "");
    }
}
