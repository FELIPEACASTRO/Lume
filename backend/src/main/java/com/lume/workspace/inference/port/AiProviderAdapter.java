package com.lume.workspace.inference.port;

import com.lume.workspace.inference.ProviderKey;
import com.lume.workspace.inference.orchestration.AiCostEstimate;
import com.lume.workspace.inference.orchestration.AiPromptCommand;
import com.lume.workspace.inference.orchestration.AiPromptResult;
import com.lume.workspace.inference.orchestration.AiProviderHealth;

public interface AiProviderAdapter {

    ProviderKey providerKey();

    AiPromptResult sendPrompt(AiPromptCommand command);

    void streamPrompt(AiPromptCommand command, AiStreamObserver observer);

    AiProviderHealth healthCheck();

    AiCostEstimate estimateCost(AiPromptCommand command);

    default boolean supportsStreaming() {
        return false;
    }
}
