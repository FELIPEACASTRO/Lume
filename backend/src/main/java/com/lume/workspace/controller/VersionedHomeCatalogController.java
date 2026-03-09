package com.lume.workspace.controller;

import com.lume.workspace.dto.CreateHomeOverviewBlockRequest;
import com.lume.workspace.dto.HomeOverviewBlockResponse;
import com.lume.workspace.dto.HomeOverviewCatalogResponse;
import com.lume.workspace.dto.HomeOverviewSettingsResponse;
import com.lume.workspace.dto.UpdateHomeOverviewBlockRequest;
import com.lume.workspace.dto.UpdateHomeOverviewSettingsRequest;
import com.lume.workspace.service.HomeCatalogService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/v1/home/catalog", "/api/v1/home/catalog"})
public class VersionedHomeCatalogController {

    private final HomeCatalogService homeCatalogService;

    public VersionedHomeCatalogController(HomeCatalogService homeCatalogService) {
        this.homeCatalogService = homeCatalogService;
    }

    @GetMapping
    public ResponseEntity<HomeOverviewCatalogResponse> catalog() {
        return ResponseEntity.ok(homeCatalogService.getCatalog());
    }

    @PatchMapping("/settings")
    public ResponseEntity<HomeOverviewSettingsResponse> updateSettings(
            @Valid @RequestBody UpdateHomeOverviewSettingsRequest request
    ) {
        return ResponseEntity.ok(homeCatalogService.updateSettings(request));
    }

    @PostMapping("/blocks")
    public ResponseEntity<HomeOverviewBlockResponse> createBlock(
            @Valid @RequestBody CreateHomeOverviewBlockRequest request
    ) {
        return ResponseEntity.status(201).body(homeCatalogService.createBlock(request));
    }

    @PatchMapping("/blocks/{id}")
    public ResponseEntity<HomeOverviewBlockResponse> updateBlock(
            @PathVariable String id,
            @Valid @RequestBody UpdateHomeOverviewBlockRequest request
    ) {
        return ResponseEntity.ok(homeCatalogService.updateBlock(id, request));
    }

    @DeleteMapping("/blocks/{id}")
    public ResponseEntity<Void> deleteBlock(@PathVariable String id) {
        homeCatalogService.deleteBlock(id);
        return ResponseEntity.noContent().build();
    }
}
