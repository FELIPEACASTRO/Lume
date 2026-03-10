package com.lume.workspace.controller;

import com.lume.workspace.dto.BootstrapSetupRequest;
import com.lume.workspace.dto.SessionContextResponse;
import com.lume.workspace.dto.SetupStatusResponse;
import com.lume.workspace.service.ApplicationSetupService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/v1/setup", "/api/v1/setup"})
public class VersionedSetupController {

    private final ApplicationSetupService applicationSetupService;

    public VersionedSetupController(ApplicationSetupService applicationSetupService) {
        this.applicationSetupService = applicationSetupService;
    }

    @GetMapping("/status")
    public ResponseEntity<SetupStatusResponse> status() {
        return ResponseEntity.ok(applicationSetupService.getStatus());
    }

    @PostMapping("/bootstrap")
    public ResponseEntity<SessionContextResponse> bootstrap(
            @Valid @RequestBody BootstrapSetupRequest request,
            HttpServletRequest httpRequest
    ) {
        ApplicationSetupService.BootstrapSetupResult result = applicationSetupService.bootstrap(request, httpRequest);
        return ResponseEntity.status(201)
                .header(HttpHeaders.SET_COOKIE, result.cookie().toString())
                .body(result.session());
    }
}
