package com.lume.workspace.service;

import com.lume.infrastructure.persistence.entity.UserJpaEntity;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

public interface WorkspaceActorOverrideResolver {

    Optional<UserJpaEntity> resolveOverride(HttpServletRequest request);
}
