package com.lume.workspace.controller;

import com.lume.workspace.dto.UnifiedInferenceRequest;
import com.lume.workspace.dto.UnifiedInferenceResponse;
import com.lume.workspace.inference.orchestration.AiStreamEvent;
import com.lume.workspace.inference.port.AiStreamObserver;
import com.lume.workspace.service.InferenceGatewayService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping({"/v1/inference", "/api/v1/inference"})
public class VersionedInferenceController {

    private final InferenceGatewayService inferenceGatewayService;

    public VersionedInferenceController(InferenceGatewayService inferenceGatewayService) {
        this.inferenceGatewayService = inferenceGatewayService;
    }

    @PostMapping("/execute")
    public ResponseEntity<UnifiedInferenceResponse> execute(@Valid @RequestBody UnifiedInferenceRequest request) {
        return ResponseEntity.ok(inferenceGatewayService.execute(request));
    }

    @PostMapping("/stream")
    public SseEmitter stream(@Valid @RequestBody UnifiedInferenceRequest request) {
        SseEmitter emitter = new SseEmitter(0L);
        CompletableFuture.runAsync(() -> inferenceGatewayService.stream(request, new SseEmitterObserver(emitter)));
        return emitter;
    }

    private static final class SseEmitterObserver implements AiStreamObserver {
        private final SseEmitter emitter;

        private SseEmitterObserver(SseEmitter emitter) {
            this.emitter = emitter;
        }

        @Override
        public void onEvent(AiStreamEvent event) {
            try {
                emitter.send(SseEmitter.event().name(event.type()).data(event.data()));
            } catch (IOException ioException) {
                onError(ioException);
            }
        }

        @Override
        public void onComplete() {
            emitter.complete();
        }

        @Override
        public void onError(Throwable error) {
            emitter.completeWithError(error);
        }
    }
}
