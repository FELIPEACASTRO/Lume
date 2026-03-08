package com.lume.workspace.controller;

import com.lume.workspace.dto.SessionContextResponse;
import com.lume.workspace.service.WorkspaceContextService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/v1/auth", "/api/v1/auth"})
public class VersionedAuthController {

    private final WorkspaceContextService workspaceContextService;

    public VersionedAuthController(WorkspaceContextService workspaceContextService) {
        this.workspaceContextService = workspaceContextService;
    }

    @GetMapping("/session")
    public ResponseEntity<SessionContextResponse> session() {
        return ResponseEntity.ok(workspaceContextService.getSession());
    }
}
