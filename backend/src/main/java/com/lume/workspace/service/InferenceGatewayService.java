package com.lume.workspace.service;

import com.lume.workspace.dto.UnifiedInferenceRequest;
import com.lume.workspace.dto.UnifiedInferenceResponse;
import com.lume.workspace.inference.orchestration.AiInferenceOrchestrator;
import com.lume.workspace.inference.port.AiStreamObserver;
import org.springframework.stereotype.Service;

@Service
public class InferenceGatewayService {

    private final AiInferenceOrchestrator inferenceOrchestrator;

    public InferenceGatewayService(AiInferenceOrchestrator inferenceOrchestrator) {
        this.inferenceOrchestrator = inferenceOrchestrator;
    }

    public UnifiedInferenceResponse execute(UnifiedInferenceRequest request) {
        return inferenceOrchestrator.execute(request);
    }

    public void stream(UnifiedInferenceRequest request, AiStreamObserver observer) {
        inferenceOrchestrator.stream(request, observer);
    }
}
