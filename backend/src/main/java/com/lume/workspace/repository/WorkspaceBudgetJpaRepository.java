package com.lume.workspace.repository;

import com.lume.workspace.entity.WorkspaceBudgetJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkspaceBudgetJpaRepository extends JpaRepository<WorkspaceBudgetJpaEntity, Long> {

    Optional<WorkspaceBudgetJpaEntity> findByWorkspaceId(Long workspaceId);
}
