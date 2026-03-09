package com.lume.workspace.service;

import com.lume.domain.exception.SetupRequiredException;
import com.lume.domain.exception.UnauthorizedException;
import com.lume.domain.service.PasswordEncoder;
import com.lume.infrastructure.persistence.entity.UserJpaEntity;
import com.lume.infrastructure.persistence.repository.JpaUserRepository;
import com.lume.workspace.dto.LoginRequest;
import com.lume.workspace.dto.SessionContextResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
public class WorkspaceAuthService {

    private final JpaUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final WorkspaceSessionService workspaceSessionService;
    private final ApplicationSetupService applicationSetupService;
    private final WorkspaceContextService workspaceContextService;

    public WorkspaceAuthService(
            JpaUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            WorkspaceSessionService workspaceSessionService,
            ApplicationSetupService applicationSetupService,
            WorkspaceContextService workspaceContextService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.workspaceSessionService = workspaceSessionService;
        this.applicationSetupService = applicationSetupService;
        this.workspaceContextService = workspaceContextService;
    }

    public AuthenticatedSession login(LoginRequest request, HttpServletRequest httpRequest) {
        if (applicationSetupService.getStatus().setupRequired()) {
            throw new SetupRequiredException("A aplicacao ainda precisa do setup inicial antes de autenticar usuarios.");
        }

        String email = request.email().trim().toLowerCase();
        UserJpaEntity user = userRepository.findByEmailAndActiveTrue(email)
                .orElseThrow(() -> new UnauthorizedException("Credenciais invalidas."));

        if (!passwordEncoder.matches(request.password().trim(), user.getPassword())) {
            throw new UnauthorizedException("Credenciais invalidas.");
        }

        ResponseCookie cookie = workspaceSessionService.createSession(user, httpRequest);
        return new AuthenticatedSession(workspaceContextService.getSessionForUser(user), cookie);
    }

    public ResponseCookie logout(HttpServletRequest request) {
        return workspaceSessionService.invalidateSession(request);
    }

    public record AuthenticatedSession(
            SessionContextResponse session,
            ResponseCookie cookie
    ) {
    }
}
