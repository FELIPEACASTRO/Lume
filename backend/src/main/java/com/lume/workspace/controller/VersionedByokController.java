package com.lume.workspace.controller;

import com.lume.workspace.dto.ByokConnectionResponse;
import com.lume.workspace.dto.CreateByokConnectionRequest;
import com.lume.workspace.dto.UpdateByokConnectionRequest;
import com.lume.workspace.service.WorkspaceByokConnectionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/byok/connections")
public class VersionedByokController {

    private final WorkspaceByokConnectionService workspaceByokConnectionService;

    public VersionedByokController(WorkspaceByokConnectionService workspaceByokConnectionService) {
        this.workspaceByokConnectionService = workspaceByokConnectionService;
    }

    @GetMapping
    public ResponseEntity<List<ByokConnectionResponse>> list() {
        return ResponseEntity.ok(workspaceByokConnectionService.listCurrentWorkspace());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ByokConnectionResponse> detail(@PathVariable String id) {
        return ResponseEntity.ok(workspaceByokConnectionService.getCurrentWorkspaceById(id));
    }

    @PostMapping
    public ResponseEntity<ByokConnectionResponse> create(
            @Valid @RequestBody CreateByokConnectionRequest request
    ) {
        return ResponseEntity.status(201).body(workspaceByokConnectionService.createCurrentWorkspace(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ByokConnectionResponse> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateByokConnectionRequest request
    ) {
        return ResponseEntity.ok(workspaceByokConnectionService.updateCurrentWorkspace(id, request));
    }

    @PostMapping("/{id}/validate")
    public ResponseEntity<ByokConnectionResponse> validate(@PathVariable String id) {
        return ResponseEntity.ok(workspaceByokConnectionService.validateCurrentWorkspace(id));
    }
}
