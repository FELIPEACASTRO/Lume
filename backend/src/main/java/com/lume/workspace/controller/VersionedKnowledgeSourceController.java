package com.lume.workspace.controller;

import com.lume.workspace.dto.CreateKnowledgeSourceRequest;
import com.lume.workspace.dto.KnowledgeSourceResponse;
import com.lume.workspace.dto.UpdateKnowledgeSourceRequest;
import com.lume.workspace.service.KnowledgeSourceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/v1/knowledge-sources", "/api/v1/knowledge-sources"})
public class VersionedKnowledgeSourceController {

    private final KnowledgeSourceService knowledgeSourceService;

    public VersionedKnowledgeSourceController(KnowledgeSourceService knowledgeSourceService) {
        this.knowledgeSourceService = knowledgeSourceService;
    }

    @GetMapping
    public ResponseEntity<List<KnowledgeSourceResponse>> knowledgeSources(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false) Boolean enabledForAgents
    ) {
        return ResponseEntity.ok(knowledgeSourceService.listSources(q, projectId, enabledForAgents));
    }

    @GetMapping("/{id}")
    public ResponseEntity<KnowledgeSourceResponse> knowledgeSource(@PathVariable String id) {
        return ResponseEntity.ok(knowledgeSourceService.findById(id));
    }

    @PostMapping
    public ResponseEntity<KnowledgeSourceResponse> createKnowledgeSource(
            @Valid @RequestBody CreateKnowledgeSourceRequest request
    ) {
        return ResponseEntity.status(201).body(knowledgeSourceService.createSource(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<KnowledgeSourceResponse> updateKnowledgeSource(
            @PathVariable String id,
            @Valid @RequestBody UpdateKnowledgeSourceRequest request
    ) {
        return ResponseEntity.ok(knowledgeSourceService.updateSource(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteKnowledgeSource(@PathVariable String id) {
        knowledgeSourceService.deleteSource(id);
        return ResponseEntity.noContent().build();
    }
}
