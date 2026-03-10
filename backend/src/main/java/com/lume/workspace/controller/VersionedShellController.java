package com.lume.workspace.controller;

import com.lume.workspace.dto.ShellNavigationResponse;
import com.lume.workspace.service.ShellNavigationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/v1/shell", "/api/v1/shell"})
public class VersionedShellController {

    private final ShellNavigationService shellNavigationService;

    public VersionedShellController(ShellNavigationService shellNavigationService) {
        this.shellNavigationService = shellNavigationService;
    }

    @GetMapping("/navigation")
    public ResponseEntity<ShellNavigationResponse> navigation() {
        return ResponseEntity.ok(shellNavigationService.getNavigation());
    }
}
