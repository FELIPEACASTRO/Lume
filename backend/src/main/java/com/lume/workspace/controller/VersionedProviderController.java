package com.lume.workspace.controller;

import com.lume.workspace.dto.ModelResponse;
import com.lume.workspace.dto.ProviderConnectivityResponse;
import com.lume.workspace.dto.ProviderCredentialResponse;
import com.lume.workspace.dto.ProviderHealthResponse;
import com.lume.workspace.dto.ProviderReadinessResponse;
import com.lume.workspace.dto.ProviderResponse;
import com.lume.workspace.dto.ProviderStatusResponse;
import com.lume.workspace.service.ProviderConnectivityService;
import com.lume.workspace.service.ProviderCatalogService;
import com.lume.workspace.service.ProviderHealthService;
import com.lume.workspace.service.WorkspaceContextService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1")
@Tag(name = "Providers", description = "AI provider catalog, credentials, status, and readiness endpoints")
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

    @Operation(summary = "List all providers", description = "Returns the full provider catalog with configuration and governance metadata.")
    @ApiResponse(responseCode = "200", description = "Provider list returned successfully")
    @GetMapping("/providers")
    public ResponseEntity<List<ProviderResponse>> providers() {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_PROVIDERS_READ);
        return ResponseEntity.ok(providerCatalogService.listProviders());
    }

    @Operation(summary = "Get provider by code", description = "Returns a single provider's details by its unique code.")
    @ApiResponse(responseCode = "200", description = "Provider returned successfully")
    @GetMapping("/providers/{provider}")
    public ResponseEntity<ProviderResponse> provider(@PathVariable String provider) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_PROVIDERS_READ);
        return ResponseEntity.ok(providerCatalogService.getProvider(provider));
    }

    @Operation(summary = "List models", description = "Returns all models, optionally filtered by provider code.")
    @ApiResponse(responseCode = "200", description = "Model list returned successfully")
    @GetMapping("/models")
    public ResponseEntity<List<ModelResponse>> models(@RequestParam(required = false) String provider) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_PROVIDERS_READ);
        return ResponseEntity.ok(providerCatalogService.listModels(provider));
    }

    @Operation(summary = "List models for provider", description = "Returns all models registered under a specific provider.")
    @ApiResponse(responseCode = "200", description = "Provider model list returned successfully")
    @GetMapping("/providers/{provider}/models")
    public ResponseEntity<List<ModelResponse>> providerModels(@PathVariable String provider) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_PROVIDERS_READ);
        return ResponseEntity.ok(providerCatalogService.listModelsForProvider(provider));
    }

    @Operation(summary = "List provider credentials", description = "Returns credential configuration and status for all providers.")
    @ApiResponse(responseCode = "200", description = "Credential list returned successfully")
    @GetMapping("/provider-credentials")
    public ResponseEntity<List<ProviderCredentialResponse>> credentials() {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_PROVIDERS_READ);
        return ResponseEntity.ok(providerCatalogService.listCredentials());
    }

    @Operation(summary = "List provider statuses", description = "Returns operational status, tier, and blocker information for all providers.")
    @ApiResponse(responseCode = "200", description = "Status list returned successfully")
    @GetMapping("/providers/status")
    public ResponseEntity<List<ProviderStatusResponse>> statuses() {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_PROVIDERS_READ);
        return ResponseEntity.ok(providerCatalogService.listProviderStatuses());
    }

    @Operation(summary = "List provider readiness", description = "Returns readiness evaluation including maturity, evidence level, and smoke status.")
    @ApiResponse(responseCode = "200", description = "Readiness list returned successfully")
    @GetMapping("/providers/readiness")
    public ResponseEntity<List<ProviderReadinessResponse>> readiness() {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_PROVIDERS_READ);
        return ResponseEntity.ok(providerCatalogService.listProviderReadiness());
    }

    @Operation(summary = "List provider health", description = "Returns live health check results for all providers.")
    @ApiResponse(responseCode = "200", description = "Health list returned successfully")
    @GetMapping("/providers/health")
    public ResponseEntity<List<ProviderHealthResponse>> health() {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_PROVIDERS_READ);
        return ResponseEntity.ok(providerHealthService.listHealth());
    }

    @Operation(summary = "Test provider connectivity", description = "Performs a live connectivity test against the specified provider.")
    @ApiResponse(responseCode = "200", description = "Connectivity test completed successfully")
    @PostMapping("/providers/{code}/connectivity-test")
    public ResponseEntity<ProviderConnectivityResponse> testConnectivity(@PathVariable String code) {
        return ResponseEntity.ok(providerConnectivityService.test(code));
    }
}
