package com.lume.workspace.controller;

import com.lume.workspace.dto.ShellCatalogItemResponse;
import com.lume.workspace.dto.ShellCatalogResponse;
import com.lume.workspace.dto.ShellCatalogTaskTypeResponse;
import com.lume.workspace.dto.CreateShellNavigationItemRequest;
import com.lume.workspace.dto.CreateShellTaskTypeRequest;
import com.lume.workspace.dto.UpdateShellNavigationItemRequest;
import com.lume.workspace.dto.UpdateShellTaskTypeRequest;
import com.lume.workspace.service.ShellNavigationService;
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
@RequestMapping({"/v1/shell/catalog", "/api/v1/shell/catalog"})
public class VersionedShellCatalogController {

    private final ShellNavigationService shellNavigationService;

    public VersionedShellCatalogController(ShellNavigationService shellNavigationService) {
        this.shellNavigationService = shellNavigationService;
    }

    @GetMapping
    public ResponseEntity<ShellCatalogResponse> catalog() {
        return ResponseEntity.ok(shellNavigationService.getCatalog());
    }

    @PostMapping("/navigation-items")
    public ResponseEntity<ShellCatalogItemResponse> createNavigationItem(
            @Valid @RequestBody CreateShellNavigationItemRequest request
    ) {
        return ResponseEntity.status(201).body(shellNavigationService.createNavigationItem(request));
    }

    @PatchMapping("/navigation-items/{id}")
    public ResponseEntity<ShellCatalogItemResponse> updateNavigationItem(
            @PathVariable String id,
            @Valid @RequestBody UpdateShellNavigationItemRequest request
    ) {
        return ResponseEntity.ok(shellNavigationService.updateNavigationItem(id, request));
    }

    @DeleteMapping("/navigation-items/{id}")
    public ResponseEntity<Void> deleteNavigationItem(@PathVariable String id) {
        shellNavigationService.deleteNavigationItem(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/task-types")
    public ResponseEntity<ShellCatalogTaskTypeResponse> createTaskType(
            @Valid @RequestBody CreateShellTaskTypeRequest request
    ) {
        return ResponseEntity.status(201).body(shellNavigationService.createTaskType(request));
    }

    @PatchMapping("/task-types/{taskType}")
    public ResponseEntity<ShellCatalogTaskTypeResponse> updateTaskType(
            @PathVariable String taskType,
            @Valid @RequestBody UpdateShellTaskTypeRequest request
    ) {
        return ResponseEntity.ok(shellNavigationService.updateTaskType(taskType, request));
    }

    @DeleteMapping("/task-types/{taskType}")
    public ResponseEntity<Void> deleteTaskType(@PathVariable String taskType) {
        shellNavigationService.deleteTaskType(taskType);
        return ResponseEntity.noContent().build();
    }
}
