package com.lume.workspace.inference.orchestration;

import com.lume.workspace.dto.UnifiedInferenceRequest;
import com.lume.workspace.dto.UnifiedInferenceResponse;
import com.lume.workspace.inference.ModelDefinition;
import com.lume.workspace.inference.ProviderDefinition;
import com.lume.workspace.inference.config.AiRuntimeProperties;
import com.lume.workspace.inference.error.AiProviderException;
import com.lume.workspace.inference.metrics.AiMetricsRecorder;
import com.lume.workspace.inference.port.AiProviderAdapter;
import com.lume.workspace.inference.port.AiStreamObserver;
import com.lume.workspace.service.ProviderCatalogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Component
public class AiInferenceOrchestrator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AiInferenceOrchestrator.class);

    private final ProviderCatalogService providerCatalogService;
    private final AiProviderRegistry providerRegistry;
    private final AiRuntimeProperties runtimeProperties;
    private final AiCircuitBreakerRegistry circuitBreakerRegistry;
    private final AiBulkheadRegistry bulkheadRegistry;
    private final AiRateLimiterRegistry rateLimiterRegistry;
    private final AiMetricsRecorder metricsRecorder;

    public AiInferenceOrchestrator(
            ProviderCatalogService providerCatalogService,
            AiProviderRegistry providerRegistry,
            AiRuntimeProperties runtimeProperties,
            AiCircuitBreakerRegistry circuitBreakerRegistry,
            AiBulkheadRegistry bulkheadRegistry,
            AiRateLimiterRegistry rateLimiterRegistry,
            AiMetricsRecorder metricsRecorder
    ) {
        this.providerCatalogService = providerCatalogService;
        this.providerRegistry = providerRegistry;
        this.runtimeProperties = runtimeProperties;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.bulkheadRegistry = bulkheadRegistry;
        this.rateLimiterRegistry = rateLimiterRegistry;
        this.metricsRecorder = metricsRecorder;
    }

    public UnifiedInferenceResponse execute(UnifiedInferenceRequest request) {
        List<String> attemptedProviderCodes = new ArrayList<>();
        List<AiExecutionAttempt> attemptChain = new ArrayList<>();
        List<String> chain = resolveProviderChain(request.providerCode(), request.fallbackProviderCodes());
        String requestedProviderCode = providerCatalogService.normalizeProviderCode(request.providerCode());
        String requestId = requestIdFor(request);
        String lastError = null;

        for (String providerCode : chain) {
            ProviderDefinition provider = providerCatalogService.requireProvider(providerCode);
            ModelDefinition model = providerCatalogService.resolveModel(provider.code(), request.modelCode());
            attemptedProviderCodes.add(provider.code());

            if (!provider.executionSupported()) {
                lastError = "O provedor " + provider.name() + " ainda nao esta liberado para uso operacional.";
                attemptChain.add(new AiExecutionAttempt(provider.code(), "unsupported", lastError, null));
                continue;
            }

            List<String> missingCredentials = providerCatalogService.missingCredentialEnvVars(provider);
            if (!missingCredentials.isEmpty()) {
                lastError = "Credenciais ausentes para " + provider.name() + ": " + String.join(", ", missingCredentials);
                attemptChain.add(new AiExecutionAttempt(provider.code(), "missing_credentials", lastError, null));
                continue;
            }

            AiProviderAdapter adapter = providerRegistry.require(provider.code());
            AiRuntimeProperties.ProviderRuntimeProperties settings = runtimeProperties.forProvider(provider.code());
            long startedAt = System.currentTimeMillis();

            try {
                circuitBreakerRegistry.beforeCall(provider.code(), settings);
                rateLimiterRegistry.beforeCall(provider.code(), settings);
                bulkheadRegistry.acquire(provider.code(), settings);
                AiPromptResult result = executeWithRetry(adapter, toCommand(request, provider.code(), model.versionLabel()), settings);
                circuitBreakerRegistry.recordSuccess(provider.code(), settings);
                long latencyMs = System.currentTimeMillis() - startedAt;
                boolean fallbackUsed = !provider.code().equalsIgnoreCase(requestedProviderCode);

                metricsRecorder.recordLatency(provider.code(), model.code(), "sendPrompt", "completed", latencyMs);
                metricsRecorder.incrementSuccess(provider.code(), model.code(), "sendPrompt");
                metricsRecorder.recordEstimatedCost(provider.code(), model.code(), "sendPrompt", result.costEstimate().estimatedCostUsd());
                if (fallbackUsed) {
                    metricsRecorder.incrementFallback(requestedProviderCode, model.code(), "sendPrompt");
                }
                attemptChain.add(new AiExecutionAttempt(provider.code(), "completed", null, latencyMs));

                LOGGER.info(
                        "Inferencia concluida requestId={} provider={} model={} routingMode={} latencyMs={} fallbackUsed={}",
                        requestId,
                        provider.code(),
                        model.code(),
                        request.routingMode(),
                        latencyMs,
                        fallbackUsed
                );

                return new UnifiedInferenceResponse(
                        result.providerCode(),
                        result.providerName(),
                        result.modelCode(),
                        result.versionLabel(),
                        result.apiStyle(),
                        true,
                        true,
                        fallbackUsed,
                        "completed",
                        result.content(),
                        null,
                        requestedProviderCode,
                        attemptedProviderCodes,
                        List.copyOf(attemptChain),
                        latencyMs,
                        result.costEstimate().estimatedInputTokens(),
                        result.costEstimate().estimatedOutputTokens(),
                        result.costEstimate().estimatedCostUsd(),
                        result.streamingSupported() ? "native" : "unsupported",
                        request.routingMode()
                );
            } catch (AiProviderException providerException) {
                circuitBreakerRegistry.recordFailure(provider.code(), settings);
                long latencyMs = System.currentTimeMillis() - startedAt;
                lastError = providerException.getMessage();
                String status = statusFromException(providerException);
                attemptChain.add(new AiExecutionAttempt(provider.code(), status, providerException.getMessage(), latencyMs));
                metricsRecorder.recordLatency(provider.code(), model.code(), "sendPrompt", status, latencyMs);
                metricsRecorder.incrementError(provider.code(), model.code(), "sendPrompt", status);
                if ("timeout".equals(status)) {
                    metricsRecorder.incrementTimeout(provider.code(), model.code(), "sendPrompt");
                }

                LOGGER.warn(
                        "Inferencia falhou requestId={} provider={} model={} routingMode={} status={} latencyMs={} detalhe={}",
                        requestId,
                        provider.code(),
                        model.code(),
                        request.routingMode(),
                        status,
                        latencyMs,
                        providerException.getMessage()
                    );
            } finally {
                bulkheadRegistry.release(provider.code());
            }
        }

        ProviderDefinition requestedProvider = providerCatalogService.requireProvider(requestedProviderCode);
        ModelDefinition requestedModel = providerCatalogService.resolveModel(requestedProvider.code(), request.modelCode());
        return new UnifiedInferenceResponse(
                requestedProvider.code(),
                requestedProvider.name(),
                requestedModel.code(),
                requestedModel.versionLabel(),
                requestedProvider.apiStyle(),
                providerCatalogService.isConfigured(requestedProvider),
                requestedProvider.executionSupported(),
                attemptedProviderCodes.size() > 1,
                determineFailureStatus(requestedProvider, lastError),
                null,
                lastError,
                requestedProviderCode,
                attemptedProviderCodes,
                List.copyOf(attemptChain),
                null,
                null,
                null,
                null,
                providerRegistry.supportsStreaming(requestedProvider.code()) ? "native" : "unsupported",
                request.routingMode()
        );
    }

    public void stream(UnifiedInferenceRequest request, AiStreamObserver observer) {
        List<String> chain = resolveProviderChain(request.providerCode(), request.fallbackProviderCodes());
        String lastError = null;

        for (String providerCode : chain) {
            ProviderDefinition provider = providerCatalogService.requireProvider(providerCode);
            ModelDefinition model = providerCatalogService.resolveModel(provider.code(), request.modelCode());
            if (!provider.executionSupported()) {
                lastError = "O provedor " + provider.name() + " ainda nao esta liberado para uso operacional.";
                continue;
            }
            List<String> missingCredentials = providerCatalogService.missingCredentialEnvVars(provider);
            if (!missingCredentials.isEmpty()) {
                lastError = "Credenciais ausentes para " + provider.name() + ": " + String.join(", ", missingCredentials);
                continue;
            }

            AiProviderAdapter adapter = providerRegistry.require(provider.code());
            if (!adapter.supportsStreaming()) {
                lastError = "Streaming nao suportado para " + provider.name() + ".";
                continue;
            }

            AiRuntimeProperties.ProviderRuntimeProperties settings = runtimeProperties.forProvider(provider.code());
            String requestId = requestIdFor(request);
            long startedAt = System.currentTimeMillis();

            try {
                circuitBreakerRegistry.beforeCall(provider.code(), settings);
                rateLimiterRegistry.beforeCall(provider.code(), settings);
                bulkheadRegistry.acquire(provider.code(), settings);
                adapter.streamPrompt(toCommand(request, provider.code(), model.versionLabel()), observer);
                circuitBreakerRegistry.recordSuccess(provider.code(), settings);
                metricsRecorder.recordLatency(provider.code(), model.code(), "streamPrompt", "completed", System.currentTimeMillis() - startedAt);
                metricsRecorder.incrementSuccess(provider.code(), model.code(), "streamPrompt");
                LOGGER.info(
                        "Streaming concluido requestId={} provider={} model={} routingMode={} latencyMs={}",
                        requestId,
                        provider.code(),
                        model.code(),
                        request.routingMode(),
                        System.currentTimeMillis() - startedAt
                );
                return;
            } catch (AiProviderException providerException) {
                circuitBreakerRegistry.recordFailure(provider.code(), settings);
                long latencyMs = System.currentTimeMillis() - startedAt;
                lastError = providerException.getMessage();
                String status = statusFromException(providerException);
                metricsRecorder.recordLatency(provider.code(), model.code(), "streamPrompt", status, latencyMs);
                metricsRecorder.incrementError(provider.code(), model.code(), "streamPrompt", status);
                if ("timeout".equals(status)) {
                    metricsRecorder.incrementTimeout(provider.code(), model.code(), "streamPrompt");
                }
                LOGGER.warn(
                        "Streaming falhou requestId={} provider={} model={} routingMode={} status={} latencyMs={} detalhe={}",
                        requestId,
                        provider.code(),
                        model.code(),
                        request.routingMode(),
                        status,
                        latencyMs,
                        providerException.getMessage()
                );
            } finally {
                bulkheadRegistry.release(provider.code());
            }
        }

        observer.onError(new AiProviderException(lastError == null ? "Streaming nao disponivel para os providers selecionados." : lastError, false));
    }

    private AiPromptResult executeWithRetry(
            AiProviderAdapter adapter,
            AiPromptCommand command,
            AiRuntimeProperties.ProviderRuntimeProperties settings
    ) {
        AiProviderException lastError = null;
        int attempts = Math.max(1, settings.getRetry().getMaxAttempts());
        long backoffMs = Math.max(0, settings.getRetry().getInitialBackoffMs());

        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                return adapter.sendPrompt(command);
            } catch (AiProviderException providerException) {
                lastError = providerException;
                if (!providerException.retryable() || attempt == attempts) {
                    throw providerException;
                }
                sleep(backoffMs);
                backoffMs = Math.min(Math.max(backoffMs * 2, 1), settings.getRetry().getMaxBackoffMs());
            }
        }

        throw lastError == null
                ? new AiProviderException("Falha generica de inferencia.", true)
                : lastError;
    }

    private AiPromptCommand toCommand(UnifiedInferenceRequest request, String effectiveProviderCode, String versionLabel) {
        List<AiMessage> messages = normalizeMessages(request);
        return new AiPromptCommand(
                requestIdFor(request),
                providerCatalogService.normalizeProviderCode(request.providerCode()),
                effectiveProviderCode,
                request.modelCode(),
                versionLabel,
                request.systemPrompt(),
                messages,
                request.temperature(),
                request.maxTokens(),
                request.routingMode(),
                request.tags(),
                request.workspaceId()
        );
    }

    private List<AiMessage> normalizeMessages(UnifiedInferenceRequest request) {
        if (request.messages() != null && !request.messages().isEmpty()) {
            return request.messages().stream()
                    .filter(message -> message != null && message.content() != null && !message.content().isBlank())
                    .map(message -> new AiMessage(message.role(), message.content()))
                    .toList();
        }
        if (request.prompt() == null || request.prompt().isBlank()) {
            return List.of();
        }
        return List.of(new AiMessage("user", request.prompt().trim()));
    }

    private List<String> resolveProviderChain(String requestedProviderCode, List<String> fallbackProviderCodes) {
        LinkedHashSet<String> chain = new LinkedHashSet<>();
        chain.add(providerCatalogService.normalizeProviderCode(requestedProviderCode));
        if (fallbackProviderCodes != null) {
            fallbackProviderCodes.stream()
                    .filter(code -> code != null && !code.isBlank())
                    .map(providerCatalogService::normalizeProviderCode)
                    .forEach(chain::add);
        }
        return List.copyOf(chain);
    }

    private String determineFailureStatus(ProviderDefinition provider, String lastError) {
        if (!provider.executionSupported()) {
            return "unsupported";
        }
        if (!providerCatalogService.missingCredentialEnvVars(provider).isEmpty()) {
            return "missing_credentials";
        }
        if (lastError == null) {
            return "provider_error";
        }
        String normalized = lastError.toLowerCase(Locale.ROOT);
        if (normalized.contains("timeout")) {
            return "timeout";
        }
        if (normalized.contains("autentic")) {
            return "authentication_error";
        }
        if (normalized.contains("circuit breaker")) {
            return "circuit_open";
        }
        if (normalized.contains("rate limit")) {
            return "rate_limited";
        }
        return "provider_error";
    }

    private String statusFromException(AiProviderException providerException) {
        String normalized = providerException.getMessage().toLowerCase(Locale.ROOT);
        if (normalized.contains("timeout")) {
            return "timeout";
        }
        if (normalized.contains("autentic")) {
            return "authentication_error";
        }
        if (normalized.contains("rate limit")) {
            return "rate_limited";
        }
        if (normalized.contains("circuit breaker")) {
            return "circuit_open";
        }
        return "provider_error";
    }

    private String requestIdFor(UnifiedInferenceRequest request) {
        if (request.requestId() != null && !request.requestId().isBlank()) {
            return request.requestId().trim();
        }
        return "req-" + UUID.randomUUID();
    }

    private void sleep(long backoffMs) {
        if (backoffMs <= 0) {
            return;
        }
        try {
            Thread.sleep(backoffMs);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new AiProviderException("Execucao interrompida durante backoff.", interruptedException, true);
        }
    }
}
