package com.lume.workspace.repository;

import com.lume.workspace.entity.WorkspaceSubscriptionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkspaceSubscriptionJpaRepository extends JpaRepository<WorkspaceSubscriptionJpaEntity, Long> {

    Optional<WorkspaceSubscriptionJpaEntity> findByWorkspaceId(Long workspaceId);
}
