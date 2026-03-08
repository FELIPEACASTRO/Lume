package com.lume.workspace.service;

import com.lume.workspace.inference.security.EnvironmentSecretResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProviderCatalogService - Unit Tests")
class ProviderCatalogServiceTest {

    @Test
    @DisplayName("should expose documented providers and credential mapping")
    void shouldExposeDocumentedProvidersAndCredentialMapping() {
        ProviderCatalogService service = service(new MockEnvironment());

        assertThat(service.listProviders())
                .extracting("code")
                .contains("openai", "anthropic", "google-gemini", "deepseek", "xai", "perplexity", "exa", "tavily", "voyage-ai", "stability-ai", "replicate", "deepgram", "assemblyai", "elevenlabs", "ideogram", "bfl", "runway", "darkowl");

        assertThat(service.listCredentials())
                .filteredOn(credential -> "openai".equals(credential.providerCode()))
                .singleElement()
                .satisfies(credential -> {
                    assertThat(credential.credentialFields()).hasSize(1);
                    assertThat(credential.credentialFields().getFirst().envVar()).isEqualTo("OPENAI_API_KEY");
                    assertThat(credential.executionSupported()).isTrue();
                    assertThat(credential.configured()).isFalse();
                    assertThat(credential.implementationStatus()).isEqualTo("live");
                    assertThat(credential.evidenceLevel()).isEqualTo("integration_verified");
                    assertThat(credential.businessPriority()).isEqualTo("high_roi");
                    assertThat(credential.apiStyle()).isEqualTo("responses");
                    assertThat(credential.streamingMode()).isEqualTo("unsupported");
                    assertThat(credential.runtimeMaturity()).isEqualTo("live");
                    assertThat(credential.pricingSummary()).contains("Custos");
                });
    }

    @Test
    @DisplayName("should report configured providers from environment")
    void shouldReportConfiguredProvidersFromEnvironment() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("OPENAI_API_KEY", "test-openai")
                .withProperty("PERPLEXITY_API_KEY", "test-pplx")
                .withProperty("VOYAGE_API_KEY", "test-voyage");

        ProviderCatalogService service = service(environment);

        assertThat(service.isConfigured("openai")).isTrue();
        assertThat(service.isConfigured("perplexity")).isTrue();
        assertThat(service.isConfigured("voyage-ai")).isTrue();
        assertThat(service.isConfigured("anthropic")).isFalse();
        assertThat(service.normalizeProviderCode("gemini")).isEqualTo("google-gemini");
        assertThat(service.normalizeProviderCode("claude")).isEqualTo("anthropic");
        assertThat(service.normalizeProviderCode("voyage")).isEqualTo("voyage-ai");
        assertThat(service.missingCredentialEnvVars("cloudflare-workers-ai"))
                .containsExactly("CLOUDFLARE_API_TOKEN", "CLOUDFLARE_ACCOUNT_ID");
        assertThat(service.getProvider("perplexity").implementationStatus()).isEqualTo("implemented_with_restrictions");
        assertThat(service.getProvider("deepgram").executionSupported()).isTrue();
        assertThat(service.getProvider("deepgram").implementationStatus()).isEqualTo("implemented_with_restrictions");
        assertThat(service.getProvider("assemblyai").executionSupported()).isTrue();
        assertThat(service.getProvider("assemblyai").implementationStatus()).isEqualTo("implemented_with_restrictions");
        assertThat(service.getProvider("elevenlabs").executionSupported()).isTrue();
        assertThat(service.getProvider("elevenlabs").implementationStatus()).isEqualTo("implemented_with_restrictions");
        assertThat(service.getProvider("ideogram").executionSupported()).isTrue();
        assertThat(service.getProvider("ideogram").implementationStatus()).isEqualTo("implemented_with_restrictions");
        assertThat(service.getProvider("bfl").executionSupported()).isTrue();
        assertThat(service.getProvider("bfl").implementationStatus()).isEqualTo("implemented_with_restrictions");
        assertThat(service.getProvider("runway").executionSupported()).isTrue();
        assertThat(service.getProvider("runway").implementationStatus()).isEqualTo("implemented_with_restrictions");
        assertThat(service.getProvider("stability-ai").executionSupported()).isTrue();
        assertThat(service.getProvider("stability-ai").implementationStatus()).isEqualTo("implemented_with_restrictions");
        assertThat(service.getProvider("replicate").executionSupported()).isTrue();
        assertThat(service.getProvider("replicate").implementationStatus()).isEqualTo("implemented_with_restrictions");
        assertThat(service.getProvider("darkowl").implementationStatus()).isEqualTo("blocked");
    }

    private ProviderCatalogService service(MockEnvironment environment) {
        return new ProviderCatalogService(new EnvironmentSecretResolver(environment));
    }
}
