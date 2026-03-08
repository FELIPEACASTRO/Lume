package com.lume.workspace.controller;

import com.lume.workspace.dto.UnifiedInferenceRequest;
import com.lume.workspace.dto.UnifiedInferenceResponse;
import com.lume.workspace.service.InferenceGatewayService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inference")
public class VersionedInferenceController {

    private final InferenceGatewayService inferenceGatewayService;

    public VersionedInferenceController(InferenceGatewayService inferenceGatewayService) {
        this.inferenceGatewayService = inferenceGatewayService;
    }

    @PostMapping("/execute")
    public ResponseEntity<UnifiedInferenceResponse> execute(@Valid @RequestBody UnifiedInferenceRequest request) {
        return ResponseEntity.ok(inferenceGatewayService.execute(request));
    }
}
