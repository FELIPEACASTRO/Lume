package com.lume.workspace.inference.orchestration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lume.workspace.dto.UnifiedInferenceRequest;
import com.lume.workspace.inference.adapter.OpenAiAdapter;
import com.lume.workspace.inference.config.AiRuntimeProperties;
import com.lume.workspace.inference.port.AiStreamObserver;
import com.lume.workspace.inference.security.EnvironmentSecretResolver;
import com.lume.workspace.service.NoOpWorkspaceLedgerService;
import com.lume.workspace.service.ProviderCatalogService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AiInferenceOrchestrator streaming")
class AiInferenceOrchestratorStreamingTest {

    @Test
    @DisplayName("should return unsupported instead of simulating chunked streaming")
    void shouldReturnUnsupportedInsteadOfSimulatingChunkedStreaming() {
        MockEnvironment environment = new MockEnvironment().withProperty("OPENAI_API_KEY", "test-openai");
        ProviderCatalogService catalogService = new ProviderCatalogService(new EnvironmentSecretResolver(environment));
        AiRuntimeProperties runtimeProperties = new AiRuntimeProperties();
        ObjectMapper objectMapper = new ObjectMapper();
        AiHttpExecutor executor = new AiHttpExecutor(RestClient.builder(), objectMapper);
        AiInferenceOrchestrator orchestrator = new AiInferenceOrchestrator(
                catalogService,
                new AiProviderRegistry(List.of(
                        new OpenAiAdapter(catalogService, executor, runtimeProperties, objectMapper)
                )),
                runtimeProperties,
                new AiCircuitBreakerRegistry(),
                new AiBulkheadRegistry(),
                new AiRateLimiterRegistry(),
                new com.lume.workspace.inference.metrics.AiMetricsRecorder(new SimpleMeterRegistry()),
                new NoOpWorkspaceLedgerService()
        );

        CapturingObserver observer = new CapturingObserver();
        orchestrator.stream(new UnifiedInferenceRequest(
                "openai",
                null,
                null,
                "Explique o status atual.",
                null,
                0.2,
                100,
                List.of(),
                "req-stream"
        ), observer);

        assertThat(observer.events).isEmpty();
        assertThat(observer.completed).isFalse();
        assertThat(observer.errorMessage).isEqualTo("Streaming nao suportado para OpenAI.");
    }

    private static final class CapturingObserver implements AiStreamObserver {
        private final java.util.List<AiStreamEvent> events = new java.util.ArrayList<>();
        private boolean completed;
        private String errorMessage;

        @Override
        public void onEvent(AiStreamEvent event) {
            events.add(event);
        }

        @Override
        public void onComplete() {
            completed = true;
        }

        @Override
        public void onError(Throwable error) {
            errorMessage = error.getMessage();
        }
    }
}
