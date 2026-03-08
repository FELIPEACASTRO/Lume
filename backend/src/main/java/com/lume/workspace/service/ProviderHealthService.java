package com.lume.workspace.service;

import com.lume.workspace.dto.ProviderHealthResponse;
import com.lume.workspace.inference.orchestration.AiProviderRegistry;
import com.lume.workspace.inference.orchestration.ProviderConnectivitySnapshot;
import com.lume.workspace.inference.port.AiProviderAdapter;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ProviderHealthService {

    private final ProviderCatalogService providerCatalogService;
    private final AiProviderRegistry providerRegistry;
    private final ProviderConnectivityService providerConnectivityService;

    public ProviderHealthService(
            ProviderCatalogService providerCatalogService,
            AiProviderRegistry providerRegistry,
            ProviderConnectivityService providerConnectivityService
    ) {
        this.providerCatalogService = providerCatalogService;
        this.providerRegistry = providerRegistry;
        this.providerConnectivityService = providerConnectivityService;
    }

    public List<ProviderHealthResponse> listHealth() {
        Map<String, ProviderConnectivitySnapshot> snapshots = providerConnectivityService.lastConnectivitySnapshots();
        return providerCatalogService.listProviders().stream()
                .map(provider -> {
                    boolean supportedByAdapter = providerRegistry.supportedProviderCodes().contains(providerCatalogService.normalizeProviderCode(provider.code()));
                    if (!supportedByAdapter) {
                        ProviderConnectivitySnapshot snapshot = snapshots.get(provider.code());
                        return new ProviderHealthResponse(
                                provider.code(),
                                provider.name(),
                                provider.category(),
                                provider.configured(),
                                provider.executionSupported(),
                                false,
                                provider.executionSupported() ? "catalog_only" : provider.catalogState(),
                                "Provider fora do runtime real desta fase.",
                                snapshot != null ? snapshot.status() : null,
                                snapshot != null ? snapshot.checkedAt().toString() : null,
                                provider.credentialFields().stream().filter(field -> field.required() && !field.configured()).map(field -> field.envVar()).toList()
                        );
                    }
                    AiProviderAdapter adapter = providerRegistry.require(provider.code());
                    var health = adapter.healthCheck();
                    ProviderConnectivitySnapshot snapshot = snapshots.get(provider.code());
                    return new ProviderHealthResponse(
                            health.providerCode(),
                            health.providerName(),
                            health.category(),
                            health.configured(),
                            health.executionSupported(),
                            health.streamingSupported(),
                            health.readinessStatus(),
                            health.message(),
                            snapshot != null ? snapshot.status() : null,
                            snapshot != null ? snapshot.checkedAt().toString() : null,
                            health.missingCredentialEnvVars()
                    );
                })
                .toList();
    }
}
