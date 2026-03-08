package com.lume.workspace.controller;

import com.lume.workspace.dto.ModelResponse;
import com.lume.workspace.dto.ProviderCredentialResponse;
import com.lume.workspace.dto.ProviderResponse;
import com.lume.workspace.service.ProviderCatalogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class VersionedProviderController {

    private final ProviderCatalogService providerCatalogService;

    public VersionedProviderController(ProviderCatalogService providerCatalogService) {
        this.providerCatalogService = providerCatalogService;
    }

    @GetMapping("/providers")
    public ResponseEntity<List<ProviderResponse>> providers() {
        return ResponseEntity.ok(providerCatalogService.listProviders());
    }

    @GetMapping("/models")
    public ResponseEntity<List<ModelResponse>> models(@RequestParam(required = false) String provider) {
        return ResponseEntity.ok(providerCatalogService.listModels(provider));
    }

    @GetMapping("/provider-credentials")
    public ResponseEntity<List<ProviderCredentialResponse>> credentials() {
        return ResponseEntity.ok(providerCatalogService.listCredentials());
    }
}
