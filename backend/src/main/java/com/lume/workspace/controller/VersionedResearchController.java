package com.lume.workspace.controller;

import com.lume.workspace.dto.ProviderResponse;
import com.lume.workspace.dto.ResearchQueryRequest;
import com.lume.workspace.dto.ResearchQueryResponse;
import com.lume.workspace.service.ProviderCatalogService;
import com.lume.workspace.service.ResearchService;
import com.lume.workspace.service.WorkspaceContextService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/v1/research", "/api/v1/research"})
public class VersionedResearchController {

    private final ProviderCatalogService providerCatalogService;
    private final ResearchService researchService;
    private final WorkspaceContextService workspaceContextService;

    public VersionedResearchController(
            ProviderCatalogService providerCatalogService,
            ResearchService researchService,
            WorkspaceContextService workspaceContextService
    ) {
        this.providerCatalogService = providerCatalogService;
        this.researchService = researchService;
        this.workspaceContextService = workspaceContextService;
    }

    @GetMapping("/providers")
    public ResponseEntity<List<ProviderResponse>> providers() {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_RESEARCH_RUN);
        return ResponseEntity.ok(providerCatalogService.listProvidersByCategory("research-search"));
    }

    @PostMapping("/query")
    public ResponseEntity<ResearchQueryResponse> query(@Valid @RequestBody ResearchQueryRequest request) {
        return ResponseEntity.ok(researchService.query(request));
    }
}
