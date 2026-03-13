package com.lume.workspace.repository;

import com.lume.workspace.entity.WorkspaceUsageEventJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface WorkspaceUsageEventJpaRepository extends JpaRepository<WorkspaceUsageEventJpaEntity, Long> {

    List<WorkspaceUsageEventJpaEntity> findByWorkspaceIdOrderByCreatedAtDesc(Long workspaceId, Pageable pageable);

    long countByWorkspaceIdAndCreatedAtAfter(Long workspaceId, LocalDateTime threshold);

    long countDistinctActorUserIdByWorkspaceIdAndActorUserIdIsNotNullAndCreatedAtAfter(Long workspaceId, LocalDateTime threshold);
}
