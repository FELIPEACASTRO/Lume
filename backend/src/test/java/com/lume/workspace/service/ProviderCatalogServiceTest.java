package com.lume.workspace.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProviderCatalogService - Unit Tests")
class ProviderCatalogServiceTest {

    @Test
    @DisplayName("should expose documented providers and credential mapping")
    void shouldExposeDocumentedProvidersAndCredentialMapping() {
        ProviderCatalogService service = new ProviderCatalogService(new MockEnvironment());

        assertThat(service.listProviders())
                .extracting("code")
                .contains("openai", "anthropic", "google-gemini", "deepseek", "xai", "perplexity", "exa", "darkowl");

        assertThat(service.listCredentials())
                .filteredOn(credential -> "openai".equals(credential.providerCode()))
                .singleElement()
                .satisfies(credential -> {
                    assertThat(credential.apiKeyEnvVar()).isEqualTo("OPENAI_API_KEY");
                    assertThat(credential.executionSupported()).isTrue();
                    assertThat(credential.configured()).isFalse();
                });
    }

    @Test
    @DisplayName("should report configured providers from environment")
    void shouldReportConfiguredProvidersFromEnvironment() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("OPENAI_API_KEY", "test-openai")
                .withProperty("PERPLEXITY_API_KEY", "test-pplx");

        ProviderCatalogService service = new ProviderCatalogService(environment);

        assertThat(service.isConfigured("openai")).isTrue();
        assertThat(service.isConfigured("perplexity")).isTrue();
        assertThat(service.isConfigured("anthropic")).isFalse();
    }
}
