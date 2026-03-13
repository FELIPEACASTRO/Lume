package com.lume.workspace.service;

import com.lume.domain.exception.ResourceNotFoundException;
import com.lume.workspace.dto.CredentialFieldResponse;
import com.lume.workspace.dto.ModelResponse;
import com.lume.workspace.dto.ProviderCredentialResponse;
import com.lume.workspace.dto.ProviderReadinessResponse;
import com.lume.workspace.dto.ProviderResponse;
import com.lume.workspace.dto.ProviderStatusResponse;
import com.lume.workspace.inference.ModelDefinition;
import com.lume.workspace.inference.ProviderDefinition;
import com.lume.workspace.inference.catalog.DefaultProviderCredentialInspector;
import com.lume.workspace.inference.catalog.ProviderGovernanceMetadata;
import com.lume.workspace.inference.catalog.ProviderGovernanceMetadataCatalog;
import com.lume.workspace.inference.catalog.DefaultProviderReadinessEvaluator;
import com.lume.workspace.inference.catalog.ProviderCredentialInspector;
import com.lume.workspace.inference.catalog.ProviderReadinessEvaluator;
import com.lume.workspace.inference.security.SecretResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class ProviderCatalogService {

    private final ProviderCredentialInspector credentialInspector;
    private final ProviderReadinessEvaluator readinessEvaluator;
    private final ProviderGovernanceMetadataCatalog governanceMetadataCatalog;
    private final ProviderCatalogRegistry registry;

    @Autowired
    public ProviderCatalogService(
            ProviderCredentialInspector credentialInspector,
            ProviderReadinessEvaluator readinessEvaluator,
            ProviderGovernanceMetadataCatalog governanceMetadataCatalog,
            ProviderCatalogRegistry registry
    ) {
        this.credentialInspector = credentialInspector;
        this.readinessEvaluator = readinessEvaluator;
        this.governanceMetadataCatalog = governanceMetadataCatalog;
        this.registry = registry;
    }

    public ProviderCatalogService(SecretResolver secretResolver) {
        this(
                new DefaultProviderCredentialInspector(secretResolver),
                new DefaultProviderReadinessEvaluator(new DefaultProviderCredentialInspector(secretResolver)),
                ProviderGovernanceMetadataCatalog.defaultCatalog(),
                new ProviderCatalogRegistry()
        );
    }

    public List<ProviderResponse> listProviders() {
        return registry.getProvidersByCode().values().stream()
                .map(this::toProviderResponse)
                .toList();
    }

    public List<ProviderResponse> listProvidersByCategory(String category) {
        return registry.getProvidersByCode().values().stream()
                .filter(provider -> provider.category().equalsIgnoreCase(category))
                .map(this::toProviderResponse)
                .toList();
    }

    public ProviderResponse getProvider(String providerCode) {
        return toProviderResponse(requireProvider(providerCode));
    }

    public List<ProviderStatusResponse> listProviderStatuses() {
        return registry.getProvidersByCode().values().stream()
                .map(provider -> {
                    ProviderGovernanceMetadata metadata = governanceMetadata(provider);
                    return new ProviderStatusResponse(
                            provider.code(),
                            provider.name(),
                            isConfigured(provider),
                            provider.executionSupported(),
                            metadata.implementationStatus(),
                            metadata.evidenceLevel(),
                            provider.catalogState(),
                            provider.category(),
                            provider.adminOnly(),
                            streamingMode(provider),
                            providerTier(provider),
                            runtimeMaturity(provider),
                            readinessStatus(provider),
                            smokeStatus(provider),
                            blockerCode(provider),
                            blockerMessage(provider),
                            missingCredentialEnvVars(provider)
                    );
                })
                .toList();
    }

    public List<ProviderReadinessResponse> listProviderReadiness() {
        return registry.getProvidersByCode().values().stream()
                .map(provider -> {
                    ProviderGovernanceMetadata metadata = governanceMetadata(provider);
                    return new ProviderReadinessResponse(
                            provider.code(),
                            provider.name(),
                            provider.category(),
                            isConfigured(provider),
                            provider.executionSupported(),
                            providerTier(provider),
                            readinessStatus(provider),
                            runtimeMaturity(provider),
                            metadata.implementationStatus(),
                            metadata.evidenceLevel(),
                            smokeStatus(provider),
                            blockerCode(provider),
                            blockerMessage(provider),
                            provider.adminOnly(),
                            missingCredentialEnvVars(provider)
                    );
                })
                .toList();
    }

    public List<ModelResponse> listModels(String providerCode) {
        if (providerCode == null || providerCode.isBlank()) {
            return registry.getModelsByCode().values().stream()
                    .map(this::toModelResponse)
                    .toList();
        }
        return registry.getModelsByProviderCode().getOrDefault(normalizeProviderCode(providerCode), List.of()).stream()
                .map(this::toModelResponse)
                .toList();
    }

    public List<ModelResponse> listModelsForProvider(String providerCode) {
        requireProvider(providerCode);
        return listModels(providerCode);
    }

    public List<ProviderCredentialResponse> listCredentials() {
        return registry.getProvidersByCode().values().stream()
                .map(provider -> {
                    ProviderGovernanceMetadata metadata = governanceMetadata(provider);
                    return new ProviderCredentialResponse(
                            provider.code(),
                            provider.name(),
                            isConfigured(provider),
                            provider.executionSupported(),
                            metadata.implementationStatus(),
                            metadata.evidenceLevel(),
                            metadata.businessPriority(),
                            metadata.syncMode(),
                            provider.category(),
                            provider.apiStyle(),
                            provider.adminOnly(),
                            streamingMode(provider),
                            providerTier(provider),
                            runtimeMaturity(provider),
                            provider.catalogState(),
                            metadata.pricingSummary(),
                            metadata.rateLimitSummary(),
                            missingCredentialEnvVars(provider),
                            toCredentialResponses(provider),
                            metadata.apiKeyPortalUrl(),
                            metadata.docsUrl(),
                            metadata.freeTierRecurring(),
                            metadata.freeTierScope(),
                            metadata.billingWarning(),
                            metadata.freeModels(),
                            metadata.regionConstraints()
                    );
                })
                .toList();
    }

    public ProviderDefinition requireProvider(String providerCode) {
        return findProvider(providerCode)
                .orElseThrow(() -> new ResourceNotFoundException("Provider", providerCode));
    }

    public Optional<ProviderDefinition> findProvider(String providerCode) {
        if (providerCode == null || providerCode.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(registry.getProvidersByCode().get(normalizeProviderCode(providerCode)));
    }

    public String normalizeProviderCode(String providerCode) {
        if (providerCode == null || providerCode.isBlank()) {
            return providerCode;
        }
        String normalized = providerCode.trim().toLowerCase(Locale.ROOT);
        return registry.getAliasesToCanonical().getOrDefault(normalized, normalized);
    }

    public ModelDefinition resolveModel(String providerCode, String modelCode) {
        if (modelCode != null && !modelCode.isBlank()) {
            ModelDefinition explicitModel = registry.getModelsByCode().get(modelCode.trim().toLowerCase());
            if (explicitModel != null) {
                return explicitModel;
            }

            ProviderDefinition provider = requireProvider(providerCode);
            return new ModelDefinition(modelCode.trim(), provider.code(), modelCode.trim(), modelCode.trim(), false, false);
        }

        ProviderDefinition provider = requireProvider(providerCode);
        ModelDefinition defaultModel = registry.getModelsByCode().get(provider.defaultModelCode().toLowerCase());
        if (defaultModel != null) {
            return defaultModel;
        }

        return new ModelDefinition(provider.defaultModelCode(), provider.code(), provider.defaultModelCode(), provider.defaultModelCode(), true, false);
    }

    public Optional<ModelDefinition> findModelForProvider(String providerCode, String modelCode) {
        if (providerCode == null || providerCode.isBlank() || modelCode == null || modelCode.isBlank()) {
            return Optional.empty();
        }

        String normalizedProvider = normalizeProviderCode(providerCode);
        return registry.getModelsByProviderCode().getOrDefault(normalizedProvider, List.of()).stream()
                .filter(model -> model.code().equalsIgnoreCase(modelCode.trim()))
                .findFirst();
    }

    public boolean isConfigured(String providerCode) {
        return findProvider(providerCode).map(this::isConfigured).orElse(false);
    }

    public boolean isConfigured(ProviderDefinition provider) {
        return credentialInspector.isConfigured(provider);
    }

    public List<String> missingCredentialEnvVars(String providerCode) {
        return findProvider(providerCode).map(this::missingCredentialEnvVars).orElse(List.of());
    }

    public List<String> missingCredentialEnvVars(ProviderDefinition provider) {
        return credentialInspector.missingCredentialEnvVars(provider);
    }

    public String credentialValue(ProviderDefinition provider, String key) {
        return credentialInspector.credentialValue(provider, key);
    }

    public String resolveBaseUrl(ProviderDefinition provider) {
        return credentialInspector.resolveBaseUrl(provider);
    }

    private ProviderResponse toProviderResponse(ProviderDefinition provider) {
        ProviderGovernanceMetadata metadata = governanceMetadata(provider);
        return new ProviderResponse(
                provider.code(),
                provider.name(),
                provider.category(),
                provider.protocol().name(),
                provider.apiStyle(),
                provider.executionSupported(),
                isConfigured(provider),
                metadata.implementationStatus(),
                metadata.evidenceLevel(),
                metadata.businessPriority(),
                metadata.syncMode(),
                provider.adminOnly(),
                provider.tenantScoped(),
                provider.supportsResponsesApi(),
                provider.supportsChatCompletions(),
                streamingMode(provider),
                providerTier(provider),
                runtimeMaturity(provider),
                provider.catalogState(),
                metadata.pricingSummary(),
                metadata.rateLimitSummary(),
                metadata.routingModes(),
                metadata.documentationSource(),
                provider.requiredHeaders(),
                toCredentialResponses(provider),
                metadata.apiKeyPortalUrl(),
                metadata.docsUrl(),
                metadata.freeTierRecurring(),
                metadata.freeTierScope(),
                metadata.billingWarning(),
                metadata.freeModels(),
                metadata.regionConstraints(),
                provider.defaultModelCode(),
                metadata.capabilities(),
                metadata.notes()
        );
    }

    private List<CredentialFieldResponse> toCredentialResponses(ProviderDefinition provider) {
        return provider.credentialFields().stream()
                .map(field -> new CredentialFieldResponse(
                        field.key(),
                        field.label(),
                        field.envVar(),
                        field.required(),
                        field.secret(),
                        credentialInspector.credentialValue(provider, field.key()) != null,
                        field.description()
                ))
                .toList();
    }

    private ModelResponse toModelResponse(ModelDefinition model) {
        ProviderDefinition provider = requireProvider(model.providerCode());
        return new ModelResponse(
                model.code(),
                model.providerCode(),
                model.label(),
                model.versionLabel(),
                provider.apiStyle(),
                provider.catalogState(),
                model.defaultModel(),
                model.enabledForAgents()
        );
    }

    public String streamingMode(String providerCode) {
        return findProvider(providerCode).map(this::streamingMode).orElse("unsupported");
    }

    public String streamingMode(ProviderDefinition provider) {
        return readinessEvaluator.streamingMode(provider);
    }

    public String runtimeMaturity(String providerCode) {
        return findProvider(providerCode).map(this::runtimeMaturity).orElse("catalog_only");
    }

    public String runtimeMaturity(ProviderDefinition provider) {
        return readinessEvaluator.runtimeMaturity(provider);
    }

    public String providerTier(String providerCode) {
        return findProvider(providerCode).map(this::providerTier).orElse("catalog_only");
    }

    public String providerTier(ProviderDefinition provider) {
        ProviderGovernanceMetadata metadata = governanceMetadata(provider);
        if (!provider.executionSupported()) {
            return "catalog_only";
        }
        if (!isConfigured(provider)) {
            return "blocked";
        }
        if (provider.adminOnly()
                || "implemented_with_restrictions".equalsIgnoreCase(metadata.implementationStatus())
                || "manual".equalsIgnoreCase(provider.catalogState())) {
            return "supported_restricted";
        }
        return "core_live";
    }

    public String readinessStatus(ProviderDefinition provider) {
        return readinessEvaluator.readinessStatus(provider);
    }

    public String smokeStatus(ProviderDefinition provider) {
        ProviderGovernanceMetadata metadata = governanceMetadata(provider);
        if ("online_verified".equalsIgnoreCase(metadata.evidenceLevel())) {
            return "verified_live";
        }
        if (!provider.executionSupported()) {
            return "not_applicable";
        }
        if (!isConfigured(provider)) {
            return "blocked";
        }
        return "pending_live_smoke";
    }

    public String blockerCode(ProviderDefinition provider) {
        if (!provider.executionSupported()) {
            return "catalog_only";
        }
        if (!isConfigured(provider)) {
            return "missing_credentials";
        }
        if (provider.adminOnly()) {
            return "admin_restricted";
        }
        ProviderGovernanceMetadata metadata = governanceMetadata(provider);
        if ("implemented_with_restrictions".equalsIgnoreCase(metadata.implementationStatus())
                || "manual".equalsIgnoreCase(provider.catalogState())) {
            return "restricted_runtime";
        }
        return null;
    }

    public String blockerMessage(ProviderDefinition provider) {
        String blockerCode = blockerCode(provider);
        if (blockerCode == null) {
            return null;
        }
        return switch (blockerCode) {
            case "catalog_only" -> "Provider catalogado, mas sem runtime automatico habilitado.";
            case "missing_credentials" -> "Provider sem credenciais suficientes no ambiente atual.";
            case "admin_restricted" -> "Provider restrito a administradores e fluxos governados.";
            case "restricted_runtime" -> "Provider com runtime parcial, manual ou sujeito a restricoes operacionais.";
            default -> "Provider com bloqueio operacional pendente.";
        };
    }

    public ProviderGovernanceMetadata governanceMetadata(String providerCode) {
        return findProvider(providerCode)
                .map(this::governanceMetadata)
                .orElse(ProviderGovernanceMetadata.empty());
    }

    public ProviderGovernanceMetadata governanceMetadata(ProviderDefinition provider) {
        return governanceMetadataCatalog.resolve(provider);
    }
}
