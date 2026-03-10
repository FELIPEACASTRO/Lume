package com.lume.workspace.inference.security;

public interface SecretResolver {

    String resolveRequired(String envVar);

    String resolveOptional(String envVar);

    boolean isConfigured(String envVar);
}
