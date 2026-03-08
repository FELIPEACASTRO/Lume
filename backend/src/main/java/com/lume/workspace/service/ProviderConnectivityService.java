package com.lume.workspace.service;

import com.lume.workspace.dto.ProviderConnectivityResponse;
import com.lume.workspace.dto.ResearchQueryRequest;
import com.lume.workspace.dto.ThreatIntelQueryRequest;
import com.lume.workspace.dto.UnifiedInferenceRequest;
import com.lume.workspace.dto.UnifiedInferenceResponse;
import com.lume.workspace.inference.ProviderDefinition;
import com.lume.workspace.inference.orchestration.ProviderConnectivitySnapshot;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ProviderConnectivityService {

    private final ProviderCatalogService providerCatalogService;
    private final WorkspaceContextService workspaceContextService;
    private final InferenceGatewayService inferenceGatewayService;
    private final ResearchService researchService;
    private final ThreatIntelService threatIntelService;
    private final Map<String, ProviderConnectivitySnapshot> snapshots;

    public ProviderConnectivityService(
            ProviderCatalogService providerCatalogService,
            WorkspaceContextService workspaceContextService,
            InferenceGatewayService inferenceGatewayService,
            ResearchService researchService,
            ThreatIntelService threatIntelService
    ) {
        this.providerCatalogService = providerCatalogService;
        this.workspaceContextService = workspaceContextService;
        this.inferenceGatewayService = inferenceGatewayService;
        this.researchService = researchService;
        this.threatIntelService = threatIntelService;
        this.snapshots = new ConcurrentHashMap<>();
    }

    public ProviderConnectivityResponse test(String providerCode) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_PROVIDERS_TEST);

        ProviderDefinition provider = providerCatalogService.requireProvider(providerCode);
        List<String> missingCredentials = providerCatalogService.missingCredentialEnvVars(provider);

        if (!missingCredentials.isEmpty()) {
            ProviderConnectivityResponse response = new ProviderConnectivityResponse(
                    provider.code(),
                    provider.name(),
                    provider.category(),
                    provider.apiStyle(),
                    "missing_credentials",
                    false,
                    provider.executionSupported(),
                    providerCatalogService.isStreamingSupported(provider),
                    null,
                    "Credenciais ausentes para o connectivity test.",
                    missingCredentials
            );
            remember(response);
            return response;
        }

        if (!provider.executionSupported()) {
            ProviderConnectivityResponse response = new ProviderConnectivityResponse(
                    provider.code(),
                    provider.name(),
                    provider.category(),
                    provider.apiStyle(),
                    "manual_setup_required",
                    providerCatalogService.isConfigured(provider),
                    false,
                    providerCatalogService.isStreamingSupported(provider),
                    null,
                    "Este provider segue manual/catalog-only nesta rodada.",
                    missingCredentials
            );
            remember(response);
            return response;
        }

        ProviderConnectivityResponse response = switch (provider.category()) {
            case "text-runtime" -> fromInference(provider, inferenceGatewayService.execute(new UnifiedInferenceRequest(
                    provider.code(),
                    provider.defaultModelCode(),
                    "Responda com a palavra OK.",
                    "ping",
                    null,
                    0.1,
                    32,
                    List.of(),
                    "connectivity-" + provider.code()
            )));
            case "research-search" -> {
                var researchResponse = researchService.query(new ResearchQueryRequest(provider.code(), "lume ai", 1));
                yield new ProviderConnectivityResponse(
                        provider.code(),
                        provider.name(),
                        provider.category(),
                        provider.apiStyle(),
                        researchResponse.status(),
                        researchResponse.configured(),
                        researchResponse.executionSupported(),
                        providerCatalogService.isStreamingSupported(provider),
                        null,
                        researchResponse.error() == null ? "Connectivity test concluido." : researchResponse.error(),
                        providerCatalogService.missingCredentialEnvVars(provider)
                );
            }
            case "threat-intel" -> {
                var threatResponse = threatIntelService.query(new ThreatIntelQueryRequest(provider.code(), "lume", 1, "Provider connectivity test"));
                yield new ProviderConnectivityResponse(
                        provider.code(),
                        provider.name(),
                        provider.category(),
                        provider.apiStyle(),
                        threatResponse.status(),
                        threatResponse.configured(),
                        threatResponse.executionSupported(),
                        providerCatalogService.isStreamingSupported(provider),
                        null,
                        threatResponse.error(),
                        providerCatalogService.missingCredentialEnvVars(provider)
                );
            }
            default -> new ProviderConnectivityResponse(
                    provider.code(),
                    provider.name(),
                    provider.category(),
                    provider.apiStyle(),
                    "unsupported",
                    providerCatalogService.isConfigured(provider),
                    provider.executionSupported(),
                    providerCatalogService.isStreamingSupported(provider),
                    null,
                    "Connectivity test ainda nao existe para esta categoria.",
                    providerCatalogService.missingCredentialEnvVars(provider)
            );
        };
        remember(response);
        return response;
    }

    private ProviderConnectivityResponse fromInference(ProviderDefinition provider, UnifiedInferenceResponse response) {
        return new ProviderConnectivityResponse(
                provider.code(),
                provider.name(),
                provider.category(),
                provider.apiStyle(),
                response.status(),
                response.configured(),
                response.executionSupported(),
                response.streamingSupported(),
                response.latencyMs(),
                response.error() == null ? "Connectivity test concluido." : response.error(),
                providerCatalogService.missingCredentialEnvVars(provider)
        );
    }

    public Map<String, ProviderConnectivitySnapshot> lastConnectivitySnapshots() {
        return Map.copyOf(snapshots);
    }

    private void remember(ProviderConnectivityResponse response) {
        snapshots.put(
                response.providerCode(),
                new ProviderConnectivitySnapshot(
                        response.providerCode(),
                        response.status(),
                        response.message(),
                        OffsetDateTime.now()
                )
        );
    }
}
