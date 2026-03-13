package com.lume.workspace.controller;

import com.lume.workspace.dto.CreateSupportTicketRequest;
import com.lume.workspace.dto.SupportTicketResponse;
import com.lume.workspace.dto.UpdateSupportTicketRequest;
import com.lume.workspace.service.SupportTicketService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/support/tickets")
public class VersionedSupportTicketController {

    private final SupportTicketService supportTicketService;

    public VersionedSupportTicketController(SupportTicketService supportTicketService) {
        this.supportTicketService = supportTicketService;
    }

    @GetMapping
    public ResponseEntity<List<SupportTicketResponse>> list(
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(supportTicketService.listCurrentWorkspace(limit));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupportTicketResponse> detail(@PathVariable String id) {
        return ResponseEntity.ok(supportTicketService.getCurrentWorkspaceTicket(id));
    }

    @PostMapping
    public ResponseEntity<SupportTicketResponse> create(
            @Valid @RequestBody CreateSupportTicketRequest request
    ) {
        return ResponseEntity.status(201).body(supportTicketService.createCurrentWorkspace(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<SupportTicketResponse> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateSupportTicketRequest request
    ) {
        return ResponseEntity.ok(supportTicketService.updateCurrentWorkspace(id, request));
    }
}
