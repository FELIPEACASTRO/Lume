package com.lume.workspace.inference.security;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class EnvironmentSecretResolver implements SecretResolver {

    private final Environment environment;

    public EnvironmentSecretResolver(Environment environment) {
        this.environment = environment;
    }

    @Override
    public String resolveRequired(String envVar) {
        String value = resolveOptional(envVar);
        if (value == null) {
            throw new IllegalStateException("A variavel de ambiente obrigatoria " + envVar + " nao foi configurada.");
        }
        return value;
    }

    @Override
    public String resolveOptional(String envVar) {
        String value = environment.getProperty(envVar);
        return value == null || value.isBlank() ? null : value.trim();
    }

    @Override
    public boolean isConfigured(String envVar) {
        return resolveOptional(envVar) != null;
    }
}
