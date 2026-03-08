package com.lume.workspace.inference.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lume.workspace.inference.ModelDefinition;
import com.lume.workspace.inference.config.AiRuntimeProperties;
import com.lume.workspace.inference.orchestration.AiHttpExecutor;
import com.lume.workspace.inference.orchestration.AiPromptCommand;
import com.lume.workspace.inference.orchestration.AiPromptResult;
import com.lume.workspace.inference.strategy.AuthStrategy;
import com.lume.workspace.inference.strategy.RequestShapeStrategy;
import com.lume.workspace.inference.strategy.ResponseExtractionStrategy;
import com.lume.workspace.service.ProviderCatalogService;

public abstract class AbstractStrategyBackedAdapter extends AbstractAiProviderAdapter {

    private final AuthStrategy authStrategy;
    private final RequestShapeStrategy requestShapeStrategy;
    private final ResponseExtractionStrategy responseExtractionStrategy;

    protected AbstractStrategyBackedAdapter(
            ProviderCatalogService providerCatalogService,
            AiHttpExecutor httpExecutor,
            AiRuntimeProperties runtimeProperties,
            ObjectMapper objectMapper,
            AuthStrategy authStrategy,
            RequestShapeStrategy requestShapeStrategy,
            ResponseExtractionStrategy responseExtractionStrategy
    ) {
        super(providerCatalogService, httpExecutor, runtimeProperties, objectMapper);
        this.authStrategy = authStrategy;
        this.requestShapeStrategy = requestShapeStrategy;
        this.responseExtractionStrategy = responseExtractionStrategy;
    }

    @Override
    public AiPromptResult sendPrompt(AiPromptCommand command) {
        ModelDefinition model = modelFor(command.modelCode());
        JsonNode payload = requestShapeStrategy.buildPayload(command, model, this, objectMapper);
        JsonNode response = postJson(
                operationPath(model),
                authStrategy.headers(provider(), providerCatalogService),
                payload
        );
        return buildResult(command, model, responseExtractionStrategy.extractContent(response));
    }

    protected abstract String operationPath(ModelDefinition model);
}
