package com.lume.workspace.controller;

import com.lume.workspace.dto.WorkspaceOptionResponse;
import com.lume.workspace.service.WorkspaceTenancyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/v1/workspaces", "/api/v1/workspaces"})
public class VersionedWorkspaceController {

    private final WorkspaceTenancyService workspaceTenancyService;

    public VersionedWorkspaceController(WorkspaceTenancyService workspaceTenancyService) {
        this.workspaceTenancyService = workspaceTenancyService;
    }

    @GetMapping
    public ResponseEntity<List<WorkspaceOptionResponse>> workspaces() {
        return ResponseEntity.ok(workspaceTenancyService.listAvailableWorkspaces());
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<Void> activate(@PathVariable Long id) {
        workspaceTenancyService.activateWorkspace(id);
        return ResponseEntity.noContent().build();
    }
}
