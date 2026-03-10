package com.lume.workspace.repository;

import com.lume.workspace.entity.WorkspaceOnboardingProfileJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkspaceOnboardingProfileJpaRepository extends JpaRepository<WorkspaceOnboardingProfileJpaEntity, Long> {

    Optional<WorkspaceOnboardingProfileJpaEntity> findByWorkspaceId(Long workspaceId);
}
