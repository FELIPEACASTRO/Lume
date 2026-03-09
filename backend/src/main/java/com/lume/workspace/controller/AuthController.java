package com.lume.workspace.controller;

import com.lume.workspace.dto.LoginRequest;
import com.lume.workspace.dto.SessionContextResponse;
import com.lume.workspace.service.WorkspaceAuthService;
import com.lume.workspace.service.WorkspaceContextService;
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
@RequestMapping("/auth")
public class AuthController {

    private final WorkspaceContextService workspaceContextService;
    private final WorkspaceAuthService workspaceAuthService;

    public AuthController(
            WorkspaceContextService workspaceContextService,
            WorkspaceAuthService workspaceAuthService
    ) {
        this.workspaceContextService = workspaceContextService;
        this.workspaceAuthService = workspaceAuthService;
    }

    @GetMapping("/session")
    public ResponseEntity<SessionContextResponse> session() {
        return ResponseEntity.ok(workspaceContextService.getSession());
    }

    @PostMapping("/login")
    public ResponseEntity<SessionContextResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        WorkspaceAuthService.AuthenticatedSession authenticatedSession = workspaceAuthService.login(request, httpRequest);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authenticatedSession.cookie().toString())
                .body(authenticatedSession.session());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest) {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, workspaceAuthService.logout(httpRequest).toString())
                .build();
    }
}
