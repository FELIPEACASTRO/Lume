package com.lume.workspace.inference.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lume.workspace.inference.config.AiRuntimeProperties;
import com.lume.workspace.inference.orchestration.AiHttpExecutor;
import com.lume.workspace.inference.port.AiProviderAdapter;
import com.lume.workspace.service.ProviderCatalogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AI Provider Adapter Contract - Unit Tests")
class AiProviderAdapterContractTest {

    @Test
    @DisplayName("all adapters should expose consistent health and streaming contract")
    void allAdaptersShouldExposeConsistentHealthAndStreamingContract() {
        MockEnvironment environment = new MockEnvironment();
        ProviderCatalogService catalogService = new ProviderCatalogService(environment);
        AiRuntimeProperties runtimeProperties = new AiRuntimeProperties();
        ObjectMapper objectMapper = new ObjectMapper();
        AiHttpExecutor executor = new AiHttpExecutor(RestClient.builder(), objectMapper);

        List<AiProviderAdapter> adapters = List.of(
                new OpenAiAdapter(catalogService, executor, runtimeProperties, objectMapper),
                new GeminiAdapter(catalogService, executor, runtimeProperties, objectMapper),
                new DeepSeekAdapter(catalogService, executor, runtimeProperties, objectMapper),
                new AnthropicAdapter(catalogService, executor, runtimeProperties, objectMapper),
                new XaiAdapter(catalogService, executor, runtimeProperties, objectMapper),
                new PerplexityAdapter(catalogService, executor, runtimeProperties, objectMapper)
        );

        adapters.forEach(adapter -> {
            var health = adapter.healthCheck();
            assertThat(health.providerCode()).isNotBlank();
            assertThat(health.executionSupported()).isTrue();
            assertThat(health.streamingSupported()).isTrue();
            assertThat(health.readinessStatus()).isEqualTo("missing_credentials");
            assertThat(health.missingCredentialEnvVars()).isNotEmpty();
        });
    }
}
