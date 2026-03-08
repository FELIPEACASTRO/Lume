package com.lume.workspace.controller;

import com.lume.workspace.dto.ModelResponse;
import com.lume.workspace.dto.ProviderConnectivityResponse;
import com.lume.workspace.dto.ProviderCredentialResponse;
import com.lume.workspace.dto.ProviderHealthResponse;
import com.lume.workspace.dto.ProviderResponse;
import com.lume.workspace.dto.ProviderStatusResponse;
import com.lume.workspace.service.ProviderConnectivityService;
import com.lume.workspace.service.ProviderCatalogService;
import com.lume.workspace.service.ProviderHealthService;
import com.lume.workspace.service.WorkspaceContextService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/v1", "/api/v1"})
public class VersionedProviderController {

    private final ProviderCatalogService providerCatalogService;
    private final ProviderConnectivityService providerConnectivityService;
    private final ProviderHealthService providerHealthService;
    private final WorkspaceContextService workspaceContextService;

    public VersionedProviderController(
            ProviderCatalogService providerCatalogService,
            ProviderConnectivityService providerConnectivityService,
            ProviderHealthService providerHealthService,
            WorkspaceContextService workspaceContextService
    ) {
        this.providerCatalogService = providerCatalogService;
        this.providerConnectivityService = providerConnectivityService;
        this.providerHealthService = providerHealthService;
        this.workspaceContextService = workspaceContextService;
    }

    @GetMapping("/providers")
    public ResponseEntity<List<ProviderResponse>> providers() {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_PROVIDERS_READ);
        return ResponseEntity.ok(providerCatalogService.listProviders());
    }

    @GetMapping("/providers/{provider}")
    public ResponseEntity<ProviderResponse> provider(@PathVariable String provider) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_PROVIDERS_READ);
        return ResponseEntity.ok(providerCatalogService.getProvider(provider));
    }

    @GetMapping("/models")
    public ResponseEntity<List<ModelResponse>> models(@RequestParam(required = false) String provider) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_PROVIDERS_READ);
        return ResponseEntity.ok(providerCatalogService.listModels(provider));
    }

    @GetMapping("/providers/{provider}/models")
    public ResponseEntity<List<ModelResponse>> providerModels(@PathVariable String provider) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_PROVIDERS_READ);
        return ResponseEntity.ok(providerCatalogService.listModelsForProvider(provider));
    }

    @GetMapping("/provider-credentials")
    public ResponseEntity<List<ProviderCredentialResponse>> credentials() {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_PROVIDERS_READ);
        return ResponseEntity.ok(providerCatalogService.listCredentials());
    }

    @GetMapping("/providers/status")
    public ResponseEntity<List<ProviderStatusResponse>> statuses() {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_PROVIDERS_READ);
        return ResponseEntity.ok(providerCatalogService.listProviderStatuses());
    }

    @GetMapping("/providers/health")
    public ResponseEntity<List<ProviderHealthResponse>> health() {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_PROVIDERS_READ);
        return ResponseEntity.ok(providerHealthService.listHealth());
    }

    @PostMapping("/providers/{code}/connectivity-test")
    public ResponseEntity<ProviderConnectivityResponse> testConnectivity(@PathVariable String code) {
        return ResponseEntity.ok(providerConnectivityService.test(code));
    }
}
