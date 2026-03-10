package com.lume.workspace.inference.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lume.workspace.inference.ModelDefinition;
import com.lume.workspace.inference.ProviderDefinition;
import com.lume.workspace.inference.config.AiRuntimeProperties;
import com.lume.workspace.inference.error.AiUnsupportedOperationException;
import com.lume.workspace.inference.orchestration.AiCostEstimate;
import com.lume.workspace.inference.orchestration.AiHttpExecutor;
import com.lume.workspace.inference.orchestration.AiMessage;
import com.lume.workspace.inference.orchestration.AiPromptCommand;
import com.lume.workspace.inference.orchestration.AiPromptResult;
import com.lume.workspace.inference.orchestration.AiProviderHealth;
import com.lume.workspace.inference.port.AiProviderAdapter;
import com.lume.workspace.inference.port.AiStreamObserver;
import com.lume.workspace.service.ProviderCatalogService;

import java.time.Duration;
import java.util.List;
import java.util.Map;

public abstract class AbstractAiProviderAdapter implements AiProviderAdapter {

    protected final ProviderCatalogService providerCatalogService;
    protected final AiHttpExecutor httpExecutor;
    protected final AiRuntimeProperties runtimeProperties;
    protected final ObjectMapper objectMapper;

    protected AbstractAiProviderAdapter(
            ProviderCatalogService providerCatalogService,
            AiHttpExecutor httpExecutor,
            AiRuntimeProperties runtimeProperties,
            ObjectMapper objectMapper
    ) {
        this.providerCatalogService = providerCatalogService;
        this.httpExecutor = httpExecutor;
        this.runtimeProperties = runtimeProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public void streamPrompt(AiPromptCommand command, AiStreamObserver observer) {
        observer.onError(new AiUnsupportedOperationException("Streaming nativo ainda nao foi implementado para " + provider().code() + "."));
    }

    @Override
    public AiProviderHealth healthCheck() {
        ProviderDefinition provider = provider();
        List<String> missingCredentials = providerCatalogService.missingCredentialEnvVars(provider);
        boolean configured = missingCredentials.isEmpty();
        boolean executionSupported = provider.executionSupported();
        String readinessStatus;
        String message;

        if (!executionSupported) {
            readinessStatus = "manual";
            message = "Integracao catalogada, mas ainda nao liberada para uso operacional.";
        } else if (configured) {
            readinessStatus = "ready";
            message = "Provider configurado e pronto para execucao.";
        } else {
            readinessStatus = "missing_credentials";
            message = "Credenciais obrigatorias ainda nao estao presentes no ambiente.";
        }

        return new AiProviderHealth(
                provider.code(),
                provider.name(),
                provider.category(),
                configured,
                executionSupported,
                supportsStreaming(),
                readinessStatus,
                message,
                missingCredentials
        );
    }

    @Override
    public AiCostEstimate estimateCost(AiPromptCommand command) {
        return estimateCost(command, command.maxTokens() != null ? command.maxTokens() : 700);
    }

    protected AiCostEstimate estimateCost(AiPromptCommand command, int estimatedOutputTokens) {
        int inputTokens = estimateInputTokens(command);
        int outputTokens = Math.max(estimatedOutputTokens, 0);
        AiRuntimeProperties.ProviderRuntimeProperties settings = runtimeProperties.forProvider(provider().code());
        Double inputCostPer1k = settings.getPricing().getInputPer1kUsd();
        Double outputCostPer1k = settings.getPricing().getOutputPer1kUsd();
        Double totalCost = null;
        if ((inputCostPer1k != null && inputCostPer1k > 0) || (outputCostPer1k != null && outputCostPer1k > 0)) {
            totalCost = ((inputTokens / 1000.0d) * (inputCostPer1k == null ? 0.0d : inputCostPer1k))
                    + ((outputTokens / 1000.0d) * (outputCostPer1k == null ? 0.0d : outputCostPer1k));
        }
        return new AiCostEstimate(inputTokens, outputTokens, totalCost);
    }

    protected AiPromptResult buildResult(AiPromptCommand command, ModelDefinition model, String content) {
        return new AiPromptResult(
                provider().code(),
                provider().name(),
                model.code(),
                model.versionLabel(),
                provider().apiStyle(),
                content == null ? "" : content.trim(),
                estimateCost(command, estimateTokens(content)),
                supportsStreaming()
        );
    }

    protected Duration timeoutFor(String providerCode) {
        return Duration.ofMillis(runtimeProperties.forProvider(providerCode).getReadTimeoutMs());
    }

    protected Duration connectTimeoutFor(String providerCode) {
        return Duration.ofMillis(runtimeProperties.forProvider(providerCode).getConnectTimeoutMs());
    }

    protected String baseUrl() {
        return providerCatalogService.resolveBaseUrl(provider());
    }

    protected ProviderDefinition provider() {
        return providerCatalogService.requireProvider(providerKey().code());
    }

    protected ModelDefinition modelFor(String modelCode) {
        return providerCatalogService.resolveModel(providerKey().code(), modelCode);
    }

    protected String externalModelCode(String internalModelCode) {
        int separator = internalModelCode.indexOf(':');
        return separator > -1 ? internalModelCode.substring(separator + 1) : internalModelCode;
    }

    protected List<AiMessage> normalizeMessages(AiPromptCommand command) {
        if (command.messages() != null && !command.messages().isEmpty()) {
            return command.messages().stream()
                    .filter(message -> message != null && message.content() != null && !message.content().isBlank())
                    .toList();
        }
        return List.of();
    }

    protected String normalizeRole(String role) {
        if ("assistant".equalsIgnoreCase(role)) {
            return "assistant";
        }
        if ("system".equalsIgnoreCase(role)) {
            return "system";
        }
        return "user";
    }

    protected JsonNode postJson(String url, Map<String, String> headers, JsonNode payload) {
        return httpExecutor.postJson(
                provider().code(),
                url,
                headers,
                payload,
                connectTimeoutFor(provider().code()),
                timeoutFor(provider().code())
        );
    }

    protected int estimateInputTokens(AiPromptCommand command) {
        StringBuilder builder = new StringBuilder();
        if (command.systemPrompt() != null) {
            builder.append(command.systemPrompt()).append('\n');
        }
        for (AiMessage message : normalizeMessages(command)) {
            builder.append(message.role()).append(':').append(message.content()).append('\n');
        }
        return estimateTokens(builder.toString());
    }

    protected int estimateTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return Math.max(1, (int) Math.ceil(text.length() / 4.0d));
    }
}
