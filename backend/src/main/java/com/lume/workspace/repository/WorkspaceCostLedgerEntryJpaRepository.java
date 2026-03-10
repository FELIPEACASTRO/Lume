package com.lume.workspace.repository;

import com.lume.workspace.entity.WorkspaceCostLedgerEntryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface WorkspaceCostLedgerEntryJpaRepository extends JpaRepository<WorkspaceCostLedgerEntryJpaEntity, Long> {

    List<WorkspaceCostLedgerEntryJpaEntity> findByWorkspaceIdOrderByCreatedAtDesc(Long workspaceId);

    long countByWorkspaceId(Long workspaceId);

    long countByWorkspaceIdAndStatus(Long workspaceId, String status);

    long countByWorkspaceIdAndCreatedAtAfter(Long workspaceId, LocalDateTime threshold);

    @Query("select coalesce(avg(e.estimatedCostUsd), 0) from WorkspaceCostLedgerEntryJpaEntity e where e.workspaceId = :workspaceId and e.estimatedCostUsd is not null")
    Double averageEstimatedCostByWorkspaceId(Long workspaceId);
}
