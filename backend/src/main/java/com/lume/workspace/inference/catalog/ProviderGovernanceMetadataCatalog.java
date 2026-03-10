package com.lume.workspace.inference.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.lume.workspace.inference.ProviderDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Locale;
import java.util.Map;

@Component
public class ProviderGovernanceMetadataCatalog {

    private static final String RESOURCE_PATH = "provider-governance-metadata.json";

    private final Map<String, ProviderGovernanceMetadata> defaultsByCategory;
    private final Map<String, ProviderGovernanceMetadata> metadataByProviderCode;

    @Autowired
    public ProviderGovernanceMetadataCatalog(ObjectMapper objectMapper) {
        this(loadPayload(objectMapper));
    }

    ProviderGovernanceMetadataCatalog(ProviderGovernanceCatalogPayload payload) {
        this.defaultsByCategory = normalizeKeys(payload.defaults());
        this.metadataByProviderCode = normalizeKeys(payload.providers());
    }

    public static ProviderGovernanceMetadataCatalog defaultCatalog() {
        ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
        return new ProviderGovernanceMetadataCatalog(loadPayload(objectMapper));
    }

    public ProviderGovernanceMetadata resolve(ProviderDefinition provider) {
        ProviderGovernanceMetadata fallback = ProviderGovernanceMetadata.fallback(provider);
        ProviderGovernanceMetadata categoryDefaults = defaultsByCategory.getOrDefault(normalize(provider.category()), ProviderGovernanceMetadata.empty());
        ProviderGovernanceMetadata overrides = metadataByProviderCode.getOrDefault(normalize(provider.code()), ProviderGovernanceMetadata.empty());
        return fallback.merge(categoryDefaults).merge(overrides);
    }

    private static ProviderGovernanceCatalogPayload loadPayload(ObjectMapper objectMapper) {
        ClassPathResource resource = new ClassPathResource(RESOURCE_PATH);
        try (InputStream inputStream = resource.getInputStream()) {
            ProviderGovernanceCatalogPayload payload = objectMapper.readValue(inputStream, ProviderGovernanceCatalogPayload.class);
            return payload == null ? new ProviderGovernanceCatalogPayload(Map.of(), Map.of()) : payload;
        } catch (IOException exception) {
            throw new UncheckedIOException("Falha ao carregar " + RESOURCE_PATH + " do classpath.", exception);
        }
    }

    private static Map<String, ProviderGovernanceMetadata> normalizeKeys(Map<String, ProviderGovernanceMetadata> input) {
        if (input == null || input.isEmpty()) {
            return Map.of();
        }
        return input.entrySet().stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        entry -> normalize(entry.getKey()),
                        Map.Entry::getValue
                ));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
