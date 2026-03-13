package com.lume.workspace.repository;

import com.lume.workspace.entity.WorkspaceFinopsReconciliationRunJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkspaceFinopsReconciliationRunJpaRepository extends JpaRepository<WorkspaceFinopsReconciliationRunJpaEntity, Long> {

    List<WorkspaceFinopsReconciliationRunJpaEntity> findByWorkspaceIdOrderByExecutedAtDesc(Long workspaceId);

    List<WorkspaceFinopsReconciliationRunJpaEntity> findByWorkspaceIdOrderByExecutedAtDesc(Long workspaceId, Pageable pageable);

    Optional<WorkspaceFinopsReconciliationRunJpaEntity> findFirstByWorkspaceIdOrderByExecutedAtDesc(Long workspaceId);
}
