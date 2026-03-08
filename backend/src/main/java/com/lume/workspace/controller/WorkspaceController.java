package com.lume.workspace.controller;

import com.lume.workspace.dto.WorkspaceSummaryResponse;
import com.lume.workspace.service.WorkspaceSummaryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/workspace")
public class WorkspaceController {

    private final WorkspaceSummaryService workspaceSummaryService;

    public WorkspaceController(WorkspaceSummaryService workspaceSummaryService) {
        this.workspaceSummaryService = workspaceSummaryService;
    }

    @GetMapping("/summary")
    public ResponseEntity<WorkspaceSummaryResponse> summary() {
        return ResponseEntity.ok(workspaceSummaryService.getSummary());
    }
}
