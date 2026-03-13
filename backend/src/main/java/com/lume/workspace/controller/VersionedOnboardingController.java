package com.lume.workspace.controller;

import com.lume.workspace.dto.UpdateWorkspaceOnboardingRequest;
import com.lume.workspace.dto.WorkspaceOnboardingResponse;
import com.lume.workspace.service.WorkspaceCommercialService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/onboarding")
public class VersionedOnboardingController {

    private final WorkspaceCommercialService workspaceCommercialService;

    public VersionedOnboardingController(WorkspaceCommercialService workspaceCommercialService) {
        this.workspaceCommercialService = workspaceCommercialService;
    }

    @GetMapping("/current")
    public ResponseEntity<WorkspaceOnboardingResponse> current() {
        return ResponseEntity.ok(workspaceCommercialService.getCurrentOnboarding());
    }

    @PatchMapping("/current")
    public ResponseEntity<WorkspaceOnboardingResponse> updateCurrent(
            @Valid @RequestBody UpdateWorkspaceOnboardingRequest request
    ) {
        return ResponseEntity.ok(workspaceCommercialService.updateCurrentOnboarding(request));
    }
}
