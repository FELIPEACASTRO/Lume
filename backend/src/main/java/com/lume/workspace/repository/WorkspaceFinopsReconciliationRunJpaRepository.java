package com.lume.workspace.repository;

import com.lume.workspace.entity.WorkspaceFinopsReconciliationRunJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkspaceFinopsReconciliationRunJpaRepository extends JpaRepository<WorkspaceFinopsReconciliationRunJpaEntity, Long> {

    List<WorkspaceFinopsReconciliationRunJpaEntity> findByWorkspaceIdOrderByExecutedAtDesc(Long workspaceId);
}
