package com.lume.workspace.controller;

import com.lume.workspace.dto.AgentProfileResponse;
import com.lume.workspace.dto.AgentConversationResponse;
import com.lume.workspace.dto.AgentMessageResponse;
import com.lume.workspace.dto.AgentThreadResponse;
import com.lume.workspace.dto.CreateAgentMessageRequest;
import com.lume.workspace.dto.CreateAgentThreadRequest;
import com.lume.workspace.dto.UpdateAgentRuntimeRequest;
import com.lume.workspace.service.AgentService;
import com.lume.workspace.service.WorkspaceContextService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/agents")
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

    @GetMapping("/threads")
    public ResponseEntity<List<AgentThreadResponse>> threads() {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_WORKSPACE_READ);
        return ResponseEntity.ok(agentService.listThreads());
    }

    @PostMapping("/threads")
    public ResponseEntity<AgentConversationResponse> createThread(
            @Valid @RequestBody CreateAgentThreadRequest request
    ) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_WORKSPACE_READ);
        return ResponseEntity.status(201).body(agentService.createThread(request));
    }

    @GetMapping("/threads/{id}/messages")
    public ResponseEntity<List<AgentMessageResponse>> messages(@PathVariable String id) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_WORKSPACE_READ);
        return ResponseEntity.ok(agentService.listMessages(id));
    }

    @PostMapping("/threads/{id}/messages")
    public ResponseEntity<AgentConversationResponse> appendMessage(
            @PathVariable String id,
            @Valid @RequestBody CreateAgentMessageRequest request
    ) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_WORKSPACE_READ);
        return ResponseEntity.ok(agentService.appendMessage(id, request));
    }

    @PatchMapping("/profiles/{id}/runtime")
    public ResponseEntity<AgentProfileResponse> updateRuntime(
            @PathVariable String id,
            @Valid @RequestBody UpdateAgentRuntimeRequest request
    ) {
        return ResponseEntity.ok(agentService.updateRuntime(id, request));
    }
}
