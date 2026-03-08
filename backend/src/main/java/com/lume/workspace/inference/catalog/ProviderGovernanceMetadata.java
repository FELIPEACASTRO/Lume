package com.lume.workspace.inference.catalog;

import com.lume.workspace.inference.ProviderDefinition;

import java.util.List;

public record ProviderGovernanceMetadata(
        String implementationStatus,
        String evidenceLevel,
        String businessPriority,
        String syncMode,
        String pricingSummary,
        String rateLimitSummary,
        List<String> routingModes,
        String documentationSource,
        String docsUrl,
        String apiKeyPortalUrl,
        List<String> capabilities,
        String notes
) {

    public static ProviderGovernanceMetadata empty() {
        return new ProviderGovernanceMetadata(
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                null,
                null,
                null,
                List.of(),
                null
        );
    }

    public static ProviderGovernanceMetadata fallback(ProviderDefinition provider) {
        return new ProviderGovernanceMetadata(
                defaultImplementationStatus(provider),
                provider.executionSupported() ? "integration_verified" : "offline_verified",
                defaultBusinessPriority(provider),
                defaultSyncMode(provider),
                "Pricing varia por fornecedor, modelo, tier e volume; acompanhar custo por workspace e capability.",
                "Rate limits variam por conta, tier e modelo; tratar como metadata operacional, nao como constante de codigo.",
                defaultRoutingModes(provider),
                defaultDocumentationSource(provider),
                provider.docsUrl(),
                provider.apiKeyPortalUrl(),
                provider.capabilities(),
                provider.notes()
        );
    }

    public ProviderGovernanceMetadata merge(ProviderGovernanceMetadata override) {
        if (override == null) {
            return this;
        }
        return new ProviderGovernanceMetadata(
                coalesce(override.implementationStatus, implementationStatus),
                coalesce(override.evidenceLevel, evidenceLevel),
                coalesce(override.businessPriority, businessPriority),
                coalesce(override.syncMode, syncMode),
                coalesce(override.pricingSummary, pricingSummary),
                coalesce(override.rateLimitSummary, rateLimitSummary),
                mergeList(routingModes, override.routingModes),
                coalesce(override.documentationSource, documentationSource),
                coalesce(override.docsUrl, docsUrl),
                coalesce(override.apiKeyPortalUrl, apiKeyPortalUrl),
                mergeList(capabilities, override.capabilities),
                coalesce(override.notes, notes)
        );
    }

    private static String defaultImplementationStatus(ProviderDefinition provider) {
        if (!provider.executionSupported()) {
            return switch (provider.catalogState()) {
                case "blocked", "out_of_scope" -> provider.catalogState();
                default -> "catalog_only";
            };
        }
        return switch (provider.category()) {
            case "media-audio", "enterprise-gateway", "threat-intel" -> "implemented_with_restrictions";
            default -> "live";
        };
    }

    private static String defaultBusinessPriority(ProviderDefinition provider) {
        return switch (provider.category()) {
            case "text-runtime", "research-search", "vector-runtime" -> "high_roi";
            case "media-audio" -> "medium_roi";
            case "enterprise-gateway", "threat-intel" -> "contract_dependent";
            default -> "medium_roi";
        };
    }

    private static String defaultSyncMode(ProviderDefinition provider) {
        return switch (provider.category()) {
            case "media-audio" -> "both";
            case "enterprise-gateway" -> "sync";
            case "threat-intel" -> "manual";
            default -> "sync";
        };
    }

    private static List<String> defaultRoutingModes(ProviderDefinition provider) {
        return switch (provider.category()) {
            case "media-audio" -> List.of("quality-first", "cost-first");
            case "threat-intel" -> List.of("quality-first");
            default -> List.of("cost-first", "latency-first", "quality-first");
        };
    }

    private static String defaultDocumentationSource(ProviderDefinition provider) {
        return switch (provider.category()) {
            case "threat-intel" -> "mixed_sources";
            default -> "primary_docs";
        };
    }

    private static String coalesce(String preferred, String fallback) {
        return preferred != null && !preferred.isBlank() ? preferred : fallback;
    }

    private static List<String> mergeList(List<String> base, List<String> override) {
        if (override != null && !override.isEmpty()) {
            return List.copyOf(override);
        }
        return base == null ? List.of() : List.copyOf(base);
    }
}
