package com.lume.workspace.controller;

import com.lume.workspace.dto.AiPlatformModels;
import com.lume.workspace.dto.ThreatIntelQueryRequest;
import com.lume.workspace.dto.ThreatIntelQueryResponse;
import com.lume.workspace.service.AiCapabilityService;
import com.lume.workspace.service.WorkspaceContextService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/v1", "/api/v1"})
public class VersionedAiPlatformController {

    private final AiCapabilityService aiCapabilityService;
    private final WorkspaceContextService workspaceContextService;

    public VersionedAiPlatformController(
            AiCapabilityService aiCapabilityService,
            WorkspaceContextService workspaceContextService
    ) {
        this.aiCapabilityService = aiCapabilityService;
        this.workspaceContextService = workspaceContextService;
    }

    @PostMapping("/chat")
    public ResponseEntity<AiPlatformModels.ChatResponse> chat(@Valid @RequestBody AiPlatformModels.ChatRequest request) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_WORKSPACE_READ);
        return ResponseEntity.ok(aiCapabilityService.chat(request));
    }

    @PostMapping("/responses")
    public ResponseEntity<AiPlatformModels.ResponseResponse> responses(@Valid @RequestBody AiPlatformModels.ResponseRequest request) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_WORKSPACE_READ);
        return ResponseEntity.ok(aiCapabilityService.responses(request));
    }

    @PostMapping("/embeddings")
    public ResponseEntity<AiPlatformModels.EmbeddingResponse> embeddings(@Valid @RequestBody AiPlatformModels.EmbeddingRequest request) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_WORKSPACE_READ);
        return ResponseEntity.ok(aiCapabilityService.embeddings(request));
    }

    @PostMapping("/rerank")
    public ResponseEntity<AiPlatformModels.RerankResponse> rerank(@Valid @RequestBody AiPlatformModels.RerankRequest request) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_WORKSPACE_READ);
        return ResponseEntity.ok(aiCapabilityService.rerank(request));
    }

    @PostMapping("/images/generate")
    public ResponseEntity<AiPlatformModels.ImageGenerationResponse> generateImage(@Valid @RequestBody AiPlatformModels.ImageGenerationRequest request) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_WORKSPACE_READ);
        return ResponseEntity.ok(aiCapabilityService.generateImage(request));
    }

    @PostMapping("/images/edit")
    public ResponseEntity<AiPlatformModels.ImageEditResponse> editImage(@Valid @RequestBody AiPlatformModels.ImageEditRequest request) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_WORKSPACE_READ);
        return ResponseEntity.ok(aiCapabilityService.editImage(request));
    }

    @GetMapping("/images/jobs/{providerCode}/{jobId}")
    public ResponseEntity<AiPlatformModels.ImageGenerationResponse> imageJobStatus(
            @PathVariable String providerCode,
            @PathVariable String jobId,
            @RequestParam(required = false) String pollingUrl
    ) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_WORKSPACE_READ);
        return ResponseEntity.ok(aiCapabilityService.imageJobStatus(providerCode, jobId, pollingUrl));
    }

    @PostMapping("/videos/generate")
    public ResponseEntity<AiPlatformModels.VideoGenerationResponse> generateVideo(@Valid @RequestBody AiPlatformModels.VideoGenerationRequest request) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_WORKSPACE_READ);
        return ResponseEntity.ok(aiCapabilityService.generateVideo(request));
    }

    @GetMapping("/videos/jobs/{providerCode}/{jobId}")
    public ResponseEntity<AiPlatformModels.VideoGenerationResponse> videoJobStatus(
            @PathVariable String providerCode,
            @PathVariable String jobId
    ) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_WORKSPACE_READ);
        return ResponseEntity.ok(aiCapabilityService.videoJobStatus(providerCode, jobId));
    }

    @PostMapping("/audio/stt")
    public ResponseEntity<AiPlatformModels.SpeechToTextResponse> speechToText(@Valid @RequestBody AiPlatformModels.SpeechToTextRequest request) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_WORKSPACE_READ);
        return ResponseEntity.ok(aiCapabilityService.speechToText(request));
    }

    @PostMapping("/audio/tts")
    public ResponseEntity<AiPlatformModels.TextToSpeechResponse> textToSpeech(@Valid @RequestBody AiPlatformModels.TextToSpeechRequest request) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_WORKSPACE_READ);
        return ResponseEntity.ok(aiCapabilityService.textToSpeech(request));
    }

    @PostMapping("/ocr")
    public ResponseEntity<AiPlatformModels.OcrResponse> ocr(@Valid @RequestBody AiPlatformModels.OcrRequest request) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_WORKSPACE_READ);
        return ResponseEntity.ok(aiCapabilityService.ocr(request));
    }

    @PostMapping("/search")
    public ResponseEntity<AiPlatformModels.AiSearchResponse> search(@Valid @RequestBody AiPlatformModels.AiSearchRequest request) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_RESEARCH_RUN);
        return ResponseEntity.ok(aiCapabilityService.search(request));
    }

    @PostMapping("/web-grounded-chat")
    public ResponseEntity<AiPlatformModels.WebGroundedChatResponse> webGroundedChat(@Valid @RequestBody AiPlatformModels.WebGroundedChatRequest request) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_WORKSPACE_READ);
        return ResponseEntity.ok(aiCapabilityService.webGroundedChat(request));
    }

    @PostMapping("/threat-intel/search")
    public ResponseEntity<ThreatIntelQueryResponse> threatIntel(@Valid @RequestBody ThreatIntelQueryRequest request) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_THREAT_INTEL_RUN);
        return ResponseEntity.ok(aiCapabilityService.threatIntelSearch(request));
    }
}
