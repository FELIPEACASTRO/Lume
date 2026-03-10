package com.lume.workspace.inference.catalog;

import com.lume.workspace.inference.ProviderDefinition;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DefaultProviderReadinessEvaluator implements ProviderReadinessEvaluator {

    private static final Set<String> NATIVE_STREAMING_PROVIDERS = Set.of();

    private final ProviderCredentialInspector credentialInspector;

    public DefaultProviderReadinessEvaluator(ProviderCredentialInspector credentialInspector) {
        this.credentialInspector = credentialInspector;
    }

    @Override
    public String readinessStatus(ProviderDefinition provider) {
        if (!provider.executionSupported()) {
            return provider.catalogState();
        }
        return credentialInspector.isConfigured(provider) ? "ready" : "missing_credentials";
    }

    @Override
    public String streamingMode(ProviderDefinition provider) {
        if (!provider.executionSupported()) {
            return "unsupported";
        }
        return NATIVE_STREAMING_PROVIDERS.contains(provider.code()) ? "native" : "unsupported";
    }

    @Override
    public String runtimeMaturity(ProviderDefinition provider) {
        if (!provider.executionSupported()) {
            return "catalog_only";
        }
        return "live";
    }
}
