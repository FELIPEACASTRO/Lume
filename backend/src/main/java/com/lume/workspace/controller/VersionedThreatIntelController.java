package com.lume.workspace.controller;

import com.lume.workspace.dto.ProviderResponse;
import com.lume.workspace.dto.ThreatIntelQueryRequest;
import com.lume.workspace.dto.ThreatIntelQueryResponse;
import com.lume.workspace.service.ProviderCatalogService;
import com.lume.workspace.service.ThreatIntelService;
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
@RequestMapping({"/v1/threat-intel", "/api/v1/threat-intel"})
public class VersionedThreatIntelController {

    private final ProviderCatalogService providerCatalogService;
    private final ThreatIntelService threatIntelService;
    private final WorkspaceContextService workspaceContextService;

    public VersionedThreatIntelController(
            ProviderCatalogService providerCatalogService,
            ThreatIntelService threatIntelService,
            WorkspaceContextService workspaceContextService
    ) {
        this.providerCatalogService = providerCatalogService;
        this.threatIntelService = threatIntelService;
        this.workspaceContextService = workspaceContextService;
    }

    @GetMapping("/providers")
    public ResponseEntity<List<ProviderResponse>> providers() {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_THREAT_INTEL_READ);
        return ResponseEntity.ok(providerCatalogService.listProvidersByCategory("threat-intel"));
    }

    @PostMapping("/query")
    public ResponseEntity<ThreatIntelQueryResponse> query(@Valid @RequestBody ThreatIntelQueryRequest request) {
        return ResponseEntity.ok(threatIntelService.query(request));
    }
}
