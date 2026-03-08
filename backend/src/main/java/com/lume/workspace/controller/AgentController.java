package com.lume.workspace.controller;

import com.lume.workspace.dto.*;
import com.lume.workspace.service.AgentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/agents")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @GetMapping("/profiles")
    public ResponseEntity<List<AgentProfileResponse>> profiles() {
        return ResponseEntity.ok(agentService.listProfiles());
    }

    @GetMapping("/threads")
    public ResponseEntity<List<AgentThreadResponse>> threads() {
        return ResponseEntity.ok(agentService.listThreads());
    }

    @PostMapping("/threads")
    public ResponseEntity<AgentConversationResponse> createThread(
            @Valid @RequestBody CreateAgentThreadRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(agentService.createThread(request));
    }

    @GetMapping("/threads/{id}/messages")
    public ResponseEntity<List<AgentMessageResponse>> messages(@PathVariable String id) {
        return ResponseEntity.ok(agentService.listMessages(id));
    }

    @PostMapping("/threads/{id}/messages")
    public ResponseEntity<AgentConversationResponse> appendMessage(
            @PathVariable String id,
            @Valid @RequestBody CreateAgentMessageRequest request
    ) {
        return ResponseEntity.ok(agentService.appendMessage(id, request));
    }
}
