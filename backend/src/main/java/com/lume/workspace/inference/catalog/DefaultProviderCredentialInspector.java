package com.lume.workspace.inference.catalog;

import com.lume.workspace.inference.CredentialFieldDefinition;
import com.lume.workspace.inference.ProviderDefinition;
import com.lume.workspace.inference.security.SecretResolver;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DefaultProviderCredentialInspector implements ProviderCredentialInspector {

    private final SecretResolver secretResolver;

    public DefaultProviderCredentialInspector(SecretResolver secretResolver) {
        this.secretResolver = secretResolver;
    }

    @Override
    public boolean isConfigured(ProviderDefinition provider) {
        return provider.credentialFields().stream()
                .filter(CredentialFieldDefinition::required)
                .allMatch(field -> secretResolver.isConfigured(field.envVar()));
    }

    @Override
    public List<String> missingCredentialEnvVars(ProviderDefinition provider) {
        return provider.credentialFields().stream()
                .filter(CredentialFieldDefinition::required)
                .filter(field -> !secretResolver.isConfigured(field.envVar()))
                .map(CredentialFieldDefinition::envVar)
                .toList();
    }

    @Override
    public String credentialValue(ProviderDefinition provider, String key) {
        return provider.credentialFields().stream()
                .filter(field -> field.key().equalsIgnoreCase(key))
                .findFirst()
                .map(field -> secretResolver.resolveOptional(field.envVar()))
                .orElse(null);
    }

    @Override
    public String resolveBaseUrl(ProviderDefinition provider) {
        String baseUrl = provider.baseUrlTemplate();
        if (baseUrl == null || baseUrl.isBlank()) {
            return baseUrl;
        }
        for (CredentialFieldDefinition field : provider.credentialFields()) {
            String placeholder = "{" + field.envVar() + "}";
            if (baseUrl.contains(placeholder)) {
                String value = secretResolver.resolveOptional(field.envVar());
                baseUrl = baseUrl.replace(placeholder, value == null ? "" : value);
            }
        }
        return baseUrl;
    }
}
