package com.lume.workspace.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lume.workspace.dto.UnifiedInferenceRequest;
import com.lume.workspace.inference.adapter.AnthropicAdapter;
import com.lume.workspace.inference.adapter.CohereAdapter;
import com.lume.workspace.inference.adapter.DeepSeekAdapter;
import com.lume.workspace.inference.adapter.DeepInfraAdapter;
import com.lume.workspace.inference.adapter.FireworksAdapter;
import com.lume.workspace.inference.adapter.GeminiAdapter;
import com.lume.workspace.inference.adapter.GroqAdapter;
import com.lume.workspace.inference.adapter.MistralAdapter;
import com.lume.workspace.inference.adapter.OpenAiAdapter;
import com.lume.workspace.inference.adapter.OpenRouterAdapter;
import com.lume.workspace.inference.adapter.PerplexityAdapter;
import com.lume.workspace.inference.adapter.TogetherAdapter;
import com.lume.workspace.inference.adapter.XaiAdapter;
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
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("real-ai")
@DisplayName("AI Real Smoke - Optional Integration Tests")
class AiRealSmokeIT {

    @Test
    @DisplayName("should execute minimal real prompts only when explicitly enabled")
    void shouldExecuteMinimalRealPromptsOnlyWhenExplicitlyEnabled() {
        Assumptions.assumeTrue("true".equalsIgnoreCase(System.getenv().getOrDefault("RUN_REAL_AI_TESTS", "false")));

        MockEnvironment environment = new MockEnvironment();
        Map.ofEntries(
                Map.entry("OPENAI_API_KEY", System.getenv("OPENAI_API_KEY")),
                Map.entry("GEMINI_API_KEY", System.getenv("GEMINI_API_KEY")),
                Map.entry("DEEPSEEK_API_KEY", System.getenv("DEEPSEEK_API_KEY")),
                Map.entry("ANTHROPIC_API_KEY", System.getenv("ANTHROPIC_API_KEY")),
                Map.entry("XAI_API_KEY", System.getenv("XAI_API_KEY")),
                Map.entry("PERPLEXITY_API_KEY", System.getenv("PERPLEXITY_API_KEY")),
                Map.entry("GROQ_API_KEY", System.getenv("GROQ_API_KEY")),
                Map.entry("OPENROUTER_API_KEY", System.getenv("OPENROUTER_API_KEY")),
                Map.entry("COHERE_API_KEY", System.getenv("COHERE_API_KEY")),
                Map.entry("TOGETHER_API_KEY", System.getenv("TOGETHER_API_KEY")),
                Map.entry("FIREWORKS_API_KEY", System.getenv("FIREWORKS_API_KEY")),
                Map.entry("DEEPINFRA_API_KEY", System.getenv("DEEPINFRA_API_KEY")),
                Map.entry("MISTRAL_API_KEY", System.getenv("MISTRAL_API_KEY"))
        ).forEach((key, value) -> {
            if (value != null && !value.isBlank()) {
                environment.setProperty(key, value);
            }
        });

        ProviderCatalogService catalogService = new ProviderCatalogService(new EnvironmentSecretResolver(environment));
        ObjectMapper objectMapper = new ObjectMapper();
        AiRuntimeProperties runtimeProperties = new AiRuntimeProperties();
        runtimeProperties.getDefaults().getRetry().setMaxAttempts(1);
        AiHttpExecutor executor = new AiHttpExecutor(RestClient.builder(), objectMapper);
        AiInferenceOrchestrator orchestrator = new AiInferenceOrchestrator(
                catalogService,
                new AiProviderRegistry(List.of(
                        new OpenAiAdapter(catalogService, executor, runtimeProperties, objectMapper),
                        new GeminiAdapter(catalogService, executor, runtimeProperties, objectMapper),
                        new DeepSeekAdapter(catalogService, executor, runtimeProperties, objectMapper),
                        new AnthropicAdapter(catalogService, executor, runtimeProperties, objectMapper),
                        new XaiAdapter(catalogService, executor, runtimeProperties, objectMapper),
                        new PerplexityAdapter(catalogService, executor, runtimeProperties, objectMapper),
                        new GroqAdapter(catalogService, executor, runtimeProperties, objectMapper),
                        new OpenRouterAdapter(catalogService, executor, runtimeProperties, objectMapper),
                        new CohereAdapter(catalogService, executor, runtimeProperties, objectMapper),
                        new TogetherAdapter(catalogService, executor, runtimeProperties, objectMapper),
                        new FireworksAdapter(catalogService, executor, runtimeProperties, objectMapper),
                        new DeepInfraAdapter(catalogService, executor, runtimeProperties, objectMapper),
                        new MistralAdapter(catalogService, executor, runtimeProperties, objectMapper)
                )),
                runtimeProperties,
                new AiCircuitBreakerRegistry(),
                new AiBulkheadRegistry(),
                new AiRateLimiterRegistry(),
                new AiMetricsRecorder(new SimpleMeterRegistry()),
                new NoOpWorkspaceLedgerService()
        );
        InferenceGatewayService service = new InferenceGatewayService(orchestrator);

        List<String> availableProviders = new ArrayList<>();
        for (String providerCode : List.of("openai", "google-gemini", "deepseek", "anthropic", "xai", "perplexity", "groq", "openrouter", "cohere", "together", "fireworks", "deepinfra", "mistral")) {
            if (catalogService.isConfigured(providerCode)) {
                availableProviders.add(providerCode);
            }
        }

        Assumptions.assumeTrue(!availableProviders.isEmpty(), "Nenhum provider real configurado no ambiente.");

        for (String providerCode : availableProviders) {
            var response = service.execute(new UnifiedInferenceRequest(
                    providerCode,
                    null,
                    "Responda apenas OK.",
                    "OK?",
                    null,
                    0.1,
                    32,
                    List.of(),
                    "real-smoke-" + providerCode
            ));

            assertThat(response.status()).isEqualTo("completed");
            assertThat(response.content()).isNotBlank();
        }
    }
}
