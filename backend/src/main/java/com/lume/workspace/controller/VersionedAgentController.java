package com.lume.workspace.controller;

import com.lume.workspace.dto.AgentProfileResponse;
import com.lume.workspace.dto.UpdateAgentRuntimeRequest;
import com.lume.workspace.service.AgentService;
import com.lume.workspace.service.WorkspaceContextService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/v1/agents", "/api/v1/agents"})
public class VersionedAgentController {

    private final AgentService agentService;
    private final WorkspaceContextService workspaceContextService;

    public VersionedAgentController(
            AgentService agentService,
            WorkspaceContextService workspaceContextService
    ) {
        this.agentService = agentService;
        this.workspaceContextService = workspaceContextService;
    }

    @GetMapping("/profiles")
    public ResponseEntity<List<AgentProfileResponse>> profiles() {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_WORKSPACE_READ);
        return ResponseEntity.ok(agentService.listProfiles());
    }

    @PatchMapping("/profiles/{id}/runtime")
    public ResponseEntity<AgentProfileResponse> updateRuntime(
            @PathVariable String id,
            @Valid @RequestBody UpdateAgentRuntimeRequest request
    ) {
        return ResponseEntity.ok(agentService.updateRuntime(id, request));
    }
}
