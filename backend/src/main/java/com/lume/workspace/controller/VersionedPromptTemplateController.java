package com.lume.workspace.controller;

import com.lume.workspace.dto.CreatePromptTemplateRequest;
import com.lume.workspace.dto.PromptTemplateResponse;
import com.lume.workspace.dto.UpdatePromptTemplateRequest;
import com.lume.workspace.service.PromptTemplateService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/v1/prompt-templates", "/api/v1/prompt-templates"})
public class VersionedPromptTemplateController {

    private final PromptTemplateService promptTemplateService;

    public VersionedPromptTemplateController(PromptTemplateService promptTemplateService) {
        this.promptTemplateService = promptTemplateService;
    }

    @GetMapping
    public ResponseEntity<List<PromptTemplateResponse>> findAll(
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "projectId", required = false) String projectId,
            @RequestParam(name = "agentProfileId", required = false) String agentProfileId,
            @RequestParam(name = "favorited", required = false) Boolean favorited
    ) {
        return ResponseEntity.ok(promptTemplateService.findAll(query, projectId, agentProfileId, favorited));
    }

    @PostMapping
    public ResponseEntity<PromptTemplateResponse> create(@Valid @RequestBody CreatePromptTemplateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(promptTemplateService.create(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<PromptTemplateResponse> update(
            @PathVariable String id,
            @Valid @RequestBody UpdatePromptTemplateRequest request
    ) {
        return ResponseEntity.ok(promptTemplateService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        promptTemplateService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/touch")
    public ResponseEntity<PromptTemplateResponse> markUsed(@PathVariable String id) {
        return ResponseEntity.ok(promptTemplateService.markUsed(id));
    }
}
