package com.lume.workspace.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lume.workspace.dto.UnifiedInferenceRequest;
import com.lume.workspace.dto.UnifiedInferenceResponse;
import com.lume.workspace.inference.adapter.AnthropicAdapter;
import com.lume.workspace.inference.adapter.OpenAiAdapter;
import com.lume.workspace.inference.config.AiRuntimeProperties;
import com.lume.workspace.inference.metrics.AiMetricsRecorder;
import com.lume.workspace.inference.orchestration.AiBulkheadRegistry;
import com.lume.workspace.inference.orchestration.AiCircuitBreakerRegistry;
import com.lume.workspace.inference.orchestration.AiHttpExecutor;
import com.lume.workspace.inference.orchestration.AiInferenceOrchestrator;
import com.lume.workspace.inference.orchestration.AiProviderRegistry;
import com.lume.workspace.inference.orchestration.AiRateLimiterRegistry;
import com.lume.workspace.inference.security.EnvironmentSecretResolver;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("InferenceGatewayService - Unit Tests")
class InferenceGatewayServiceTest {

    @Test
    @DisplayName("should use fallback provider only when requested")
    void shouldUseFallbackProviderOnlyWhenRequested() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("OPENAI_API_KEY", "test-openai")
                .withProperty("ANTHROPIC_API_KEY", "test-anthropic");
        ProviderCatalogService catalogService = catalogService(environment);
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();

        server.expect(requestTo("https://api.openai.com/v1/responses"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-openai"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).contentType(MediaType.APPLICATION_JSON).body("{\"error\":\"rate limited\"}"));

        server.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-api-key", "test-anthropic"))
                .andRespond(withSuccess("""
                        {"content":[{"type":"text","text":"Resposta Claude"}]}
                        """, MediaType.APPLICATION_JSON));

        AiRuntimeProperties runtimeProperties = new AiRuntimeProperties();
        runtimeProperties.getDefaults().getRetry().setMaxAttempts(1);
        ObjectMapper objectMapper = new ObjectMapper();
        AiHttpExecutor executor = new AiHttpExecutor(builder, objectMapper);
        AiInferenceOrchestrator orchestrator = new AiInferenceOrchestrator(
                catalogService,
                new AiProviderRegistry(List.of(
                        new OpenAiAdapter(catalogService, executor, runtimeProperties, objectMapper),
                        new AnthropicAdapter(catalogService, executor, runtimeProperties, objectMapper)
                )),
                runtimeProperties,
                new AiCircuitBreakerRegistry(),
                new AiBulkheadRegistry(),
                new AiRateLimiterRegistry(),
                new AiMetricsRecorder(new SimpleMeterRegistry()),
                new NoOpWorkspaceLedgerService()
        );
        InferenceGatewayService service = new InferenceGatewayService(orchestrator);

        UnifiedInferenceResponse response = service.execute(new UnifiedInferenceRequest(
                "openai",
                null,
                "Seja direto.",
                "Resuma o estado do workspace.",
                null,
                0.2,
                200,
                List.of("anthropic"),
                "req-fallback"
        ));

        assertThat(response.status()).isEqualTo("completed");
        assertThat(response.providerCode()).isEqualTo("anthropic");
        assertThat(response.requestedProviderCode()).isEqualTo("openai");
        assertThat(response.fallbackUsed()).isTrue();
        assertThat(response.attemptedProviderCodes()).containsExactly("openai", "anthropic");
        assertThat(response.attemptChain()).hasSize(2);
        assertThat(response.attemptChain().get(0).status()).isEqualTo("rate_limited");
        assertThat(response.attemptChain().get(1).status()).isEqualTo("completed");
        assertThat(response.content()).isEqualTo("Resposta Claude");
    }

    @Test
    @DisplayName("should return missing credentials when provider key is absent")
    void shouldReturnMissingCredentialsWhenProviderKeyIsAbsent() {
        ProviderCatalogService catalogService = catalogService(new MockEnvironment());
        AiInferenceOrchestrator orchestrator = new AiInferenceOrchestrator(
                catalogService,
                new AiProviderRegistry(List.of(
                        new OpenAiAdapter(catalogService, new AiHttpExecutor(RestClient.builder(), new ObjectMapper()), new AiRuntimeProperties(), new ObjectMapper())
                )),
                new AiRuntimeProperties(),
                new AiCircuitBreakerRegistry(),
                new AiBulkheadRegistry(),
                new AiRateLimiterRegistry(),
                new AiMetricsRecorder(new SimpleMeterRegistry()),
                new NoOpWorkspaceLedgerService()
        );
        InferenceGatewayService service = new InferenceGatewayService(orchestrator);

        UnifiedInferenceResponse response = service.execute(new UnifiedInferenceRequest(
                "openai",
                null,
                null,
                "Hello",
                null,
                null,
                null,
                List.of(),
                "req-missing"
        ));

        assertThat(response.status()).isEqualTo("missing_credentials");
        assertThat(response.configured()).isFalse();
        assertThat(response.content()).isNull();
        assertThat(response.attemptChain()).hasSize(1);
        assertThat(response.attemptChain().get(0).status()).isEqualTo("missing_credentials");
        assertThat(response.streamingMode()).isEqualTo("unsupported");
    }

    private ProviderCatalogService catalogService(MockEnvironment environment) {
        return new ProviderCatalogService(new EnvironmentSecretResolver(environment));
    }
}
