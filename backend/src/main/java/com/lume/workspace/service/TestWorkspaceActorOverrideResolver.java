package com.lume.workspace.service;

import com.lume.domain.exception.AccessDeniedException;
import com.lume.domain.exception.UnauthorizedException;
import com.lume.infrastructure.config.WorkspaceAuthProperties;
import com.lume.infrastructure.persistence.entity.UserJpaEntity;
import com.lume.infrastructure.persistence.repository.JpaUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Profile("test")
public class TestWorkspaceActorOverrideResolver implements WorkspaceActorOverrideResolver {

    private final JpaUserRepository userRepository;
    private final WorkspaceAuthProperties workspaceAuthProperties;

    public TestWorkspaceActorOverrideResolver(
            JpaUserRepository userRepository,
            WorkspaceAuthProperties workspaceAuthProperties
    ) {
        this.userRepository = userRepository;
        this.workspaceAuthProperties = workspaceAuthProperties;
    }

    @Override
    public Optional<UserJpaEntity> resolveOverride(HttpServletRequest request) {
        if (request == null) {
            return Optional.empty();
        }

        if (workspaceAuthProperties.isAllowTestHeader()) {
            String actorUserId = request.getHeader(WorkspaceContextService.HEADER_ACTOR_USER_ID);
            if (actorUserId != null && !actorUserId.isBlank()) {
                try {
                    Long parsedId = Long.valueOf(actorUserId.trim());
                    return Optional.of(
                            userRepository.findByIdAndActiveTrue(parsedId)
                                    .orElseThrow(() -> new AccessDeniedException("O usuario informado no header nao esta ativo."))
                    );
                } catch (NumberFormatException exception) {
                    throw new AccessDeniedException("O header de usuario atual e invalido.");
                }
            }
        }

        if (workspaceAuthProperties.isAllowTestAutoLogin()) {
            return Optional.of(
                    userRepository.findTopByActiveTrueOrderByCreatedAtAsc()
                            .orElseThrow(() -> new UnauthorizedException("Nenhum usuario ativo esta disponivel para auto-login em teste."))
            );
        }

        return Optional.empty();
    }
}
