package com.lume.workspace.controller;

import com.lume.workspace.dto.MultiInferenceRequest;
import com.lume.workspace.dto.MultiInferenceResponse;
import com.lume.workspace.dto.UnifiedInferenceRequest;
import com.lume.workspace.dto.UnifiedInferenceResponse;
import com.lume.workspace.inference.orchestration.AiStreamEvent;
import com.lume.workspace.inference.port.AiStreamObserver;
import com.lume.workspace.service.InferenceGatewayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Tag(name = "Inference", description = "AI inference execution and streaming endpoints")
@RestController
@RequestMapping("/v1/inference")
public class VersionedInferenceController {

    private final InferenceGatewayService inferenceGatewayService;

    public VersionedInferenceController(InferenceGatewayService inferenceGatewayService) {
        this.inferenceGatewayService = inferenceGatewayService;
    }

    @Operation(summary = "Execute synchronous inference",
               description = "Sends a prompt to the specified AI provider and returns the complete response.")
    @ApiResponse(responseCode = "200", description = "Inference executed successfully")
    @PostMapping("/execute")
    public ResponseEntity<UnifiedInferenceResponse> execute(@Valid @RequestBody UnifiedInferenceRequest request) {
        return ResponseEntity.ok(inferenceGatewayService.execute(request));
    }

    @Operation(summary = "Stream inference via SSE",
               description = "Sends a prompt to the specified AI provider and streams the response as Server-Sent Events.")
    @ApiResponse(responseCode = "200", description = "SSE stream opened successfully")
    @PostMapping("/stream")
    public SseEmitter stream(@Valid @RequestBody UnifiedInferenceRequest request) {
        SseEmitter emitter = new SseEmitter(120_000L);
        CompletableFuture.runAsync(() -> inferenceGatewayService.stream(request, new SseEmitterObserver(emitter)));
        return emitter;
    }

    @Operation(summary = "Execute multi-provider inference",
               description = "Sends a prompt to multiple AI providers in parallel and returns all responses.")
    @ApiResponse(responseCode = "200", description = "Multi-provider inference executed successfully")
    @PostMapping("/multi-execute")
    public ResponseEntity<MultiInferenceResponse> multiExecute(@Valid @RequestBody MultiInferenceRequest request) {
        return ResponseEntity.ok(inferenceGatewayService.executeMulti(request));
    }

    @Operation(summary = "Stream multi-provider inference via SSE",
               description = "Sends a prompt to multiple AI providers in parallel and streams all responses as Server-Sent Events.")
    @ApiResponse(responseCode = "200", description = "Multi-provider SSE stream opened successfully")
    @PostMapping("/multi-stream")
    public SseEmitter multiStream(@Valid @RequestBody MultiInferenceRequest request) {
        SseEmitter emitter = new SseEmitter(120_000L);
        CompletableFuture.runAsync(() -> inferenceGatewayService.streamMulti(request, new SseEmitterObserver(emitter)));
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
