package com.lume.workspace.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.lume.workspace.inference.ProviderDefinition;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

@Service
public class GoogleCloudSupportService {

    private static final List<String> SCOPES = List.of("https://www.googleapis.com/auth/cloud-platform");

    private final ProviderCatalogService providerCatalogService;
    private final ObjectMapper objectMapper;

    public GoogleCloudSupportService(
            ProviderCatalogService providerCatalogService,
            ObjectMapper objectMapper
    ) {
        this.providerCatalogService = providerCatalogService;
        this.objectMapper = objectMapper;
    }

    public GoogleAccessContext accessContext(ProviderDefinition provider) {
        String credentialsJson = providerCatalogService.credentialValue(provider, "credentialsJson");
        if (credentialsJson == null || credentialsJson.isBlank()) {
            throw new IllegalStateException("Credenciais Google Cloud ausentes.");
        }

        try {
            JsonNode jsonNode = objectMapper.readTree(credentialsJson);
            String projectId = jsonNode.path("project_id").asText(null);
            if (projectId == null || projectId.isBlank()) {
                throw new IllegalStateException("As credenciais Google Cloud nao informam project_id.");
            }

            GoogleCredentials credentials = GoogleCredentials
                    .fromStream(new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8)))
                    .createScoped(SCOPES);

            AccessToken token = credentials.getAccessToken();
            if (token == null || token.getExpirationTime() == null || token.getExpirationTime().toInstant().isBefore(Instant.now().plusSeconds(30))) {
                token = credentials.refreshAccessToken();
            }
            return new GoogleAccessContext(token.getTokenValue(), projectId);
        } catch (IOException exception) {
            throw new IllegalStateException("Falha ao ler credenciais Google Cloud.", exception);
        }
    }

    public record GoogleAccessContext(
            String accessToken,
            String projectId
    ) {
    }
}
