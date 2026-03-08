package com.lume.workspace.service;

import com.lume.workspace.dto.ProviderConnectivityResponse;
import com.lume.workspace.dto.ResearchQueryRequest;
import com.lume.workspace.dto.ThreatIntelQueryRequest;
import com.lume.workspace.dto.UnifiedInferenceRequest;
import com.lume.workspace.dto.UnifiedInferenceResponse;
import com.lume.workspace.inference.ProviderDefinition;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProviderConnectivityService {

    private final ProviderCatalogService providerCatalogService;
    private final WorkspaceContextService workspaceContextService;
    private final InferenceGatewayService inferenceGatewayService;
    private final ResearchService researchService;
    private final ThreatIntelService threatIntelService;

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
    }

    public ProviderConnectivityResponse test(String providerCode) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_PROVIDERS_TEST);

        ProviderDefinition provider = providerCatalogService.requireProvider(providerCode);
        List<String> missingCredentials = providerCatalogService.missingCredentialEnvVars(provider);

        if (!missingCredentials.isEmpty()) {
            return new ProviderConnectivityResponse(provider.code(), provider.name(), provider.category(), provider.apiStyle(), "missing_credentials", false, provider.executionSupported(), "Credenciais ausentes para o connectivity test.", missingCredentials);
        }

        if (!provider.executionSupported()) {
            return new ProviderConnectivityResponse(provider.code(), provider.name(), provider.category(), provider.apiStyle(), "manual_setup_required", providerCatalogService.isConfigured(provider), false, "Este provider segue manual/catalog-only nesta rodada.", missingCredentials);
        }

        return switch (provider.category()) {
            case "text-runtime" -> fromInference(provider, inferenceGatewayService.execute(new UnifiedInferenceRequest(
                    provider.code(),
                    provider.defaultModelCode(),
                    "Responda com a palavra OK.",
                    "ping",
                    null,
                    0.1,
                    32
            )));
            case "research-search" -> {
                var response = researchService.query(new ResearchQueryRequest(provider.code(), "lume ai", 1));
                yield new ProviderConnectivityResponse(provider.code(), provider.name(), provider.category(), provider.apiStyle(), response.status(), response.configured(), response.executionSupported(), response.error() == null ? "Connectivity test concluido." : response.error(), providerCatalogService.missingCredentialEnvVars(provider));
            }
            case "threat-intel" -> {
                var response = threatIntelService.query(new ThreatIntelQueryRequest(provider.code(), "lume", 1));
                yield new ProviderConnectivityResponse(provider.code(), provider.name(), provider.category(), provider.apiStyle(), response.status(), response.configured(), response.executionSupported(), response.error(), providerCatalogService.missingCredentialEnvVars(provider));
            }
            default -> new ProviderConnectivityResponse(provider.code(), provider.name(), provider.category(), provider.apiStyle(), "unsupported", providerCatalogService.isConfigured(provider), provider.executionSupported(), "Connectivity test ainda nao existe para esta categoria.", providerCatalogService.missingCredentialEnvVars(provider));
        };
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
                response.error() == null ? "Connectivity test concluido." : response.error(),
                providerCatalogService.missingCredentialEnvVars(provider)
        );
    }
}
