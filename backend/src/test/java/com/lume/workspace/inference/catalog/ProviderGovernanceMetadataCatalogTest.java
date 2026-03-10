package com.lume.workspace.inference.catalog;

import com.lume.workspace.inference.CredentialFieldDefinition;
import com.lume.workspace.inference.InferenceProtocol;
import com.lume.workspace.inference.ProviderDefinition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Provider governance metadata catalog")
class ProviderGovernanceMetadataCatalogTest {

    @Test
    @DisplayName("should load category defaults and provider overrides from classpath metadata")
    void shouldLoadCategoryDefaultsAndProviderOverridesFromClasspathMetadata() {
        ProviderGovernanceMetadataCatalog catalog = ProviderGovernanceMetadataCatalog.defaultCatalog();

        ProviderDefinition openai = provider("openai", "text-runtime", true, "live");
        ProviderDefinition darkowl = provider("darkowl", "threat-intel", false, "manual");

        ProviderGovernanceMetadata openaiMetadata = catalog.resolve(openai);
        ProviderGovernanceMetadata darkowlMetadata = catalog.resolve(darkowl);

        assertThat(openaiMetadata.implementationStatus()).isEqualTo("live");
        assertThat(openaiMetadata.evidenceLevel()).isEqualTo("integration_verified");
        assertThat(openaiMetadata.businessPriority()).isEqualTo("high_roi");
        assertThat(openaiMetadata.routingModes()).contains("cost-first", "latency-first", "quality-first");

        assertThat(darkowlMetadata.implementationStatus()).isEqualTo("blocked");
        assertThat(darkowlMetadata.evidenceLevel()).isEqualTo("offline_verified");
        assertThat(darkowlMetadata.documentationSource()).isEqualTo("mixed_sources");
    }

    private ProviderDefinition provider(String code, String category, boolean executionSupported, String catalogState) {
        return new ProviderDefinition(
                code,
                code,
                category,
                InferenceProtocol.OPENAI_RESPONSES,
                executionSupported,
                "https://example.com",
                List.of(new CredentialFieldDefinition("apiKey", "API Key", "EXAMPLE_API_KEY", true, true, "test")),
                "bearer",
                "responses",
                List.of("Authorization"),
                false,
                true,
                true,
                false,
                catalogState,
                "https://portal.example.com",
                "https://docs.example.com",
                code + ":default",
                List.of("chat"),
                "provider notes"
        );
    }
}
