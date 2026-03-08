package com.lume.workspace.inference.catalog;

import com.lume.workspace.inference.ProviderDefinition;

import java.util.List;

public interface ProviderCredentialInspector {

    boolean isConfigured(ProviderDefinition provider);

    List<String> missingCredentialEnvVars(ProviderDefinition provider);

    String credentialValue(ProviderDefinition provider, String key);

    String resolveBaseUrl(ProviderDefinition provider);
}
