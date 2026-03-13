package com.lume.workspace.repository;

import com.lume.workspace.entity.WorkspaceByokConnectionJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkspaceByokConnectionJpaRepository extends JpaRepository<WorkspaceByokConnectionJpaEntity, String> {

    List<WorkspaceByokConnectionJpaEntity> findByWorkspaceIdOrderByUpdatedAtDesc(Long workspaceId);

    List<WorkspaceByokConnectionJpaEntity> findByWorkspaceIdOrderByUpdatedAtDesc(Long workspaceId, Pageable pageable);

    Optional<WorkspaceByokConnectionJpaEntity> findByIdAndWorkspaceId(String id, Long workspaceId);

    boolean existsByWorkspaceIdAndConnectionName(Long workspaceId, String connectionName);

    long countByWorkspaceId(Long workspaceId);

    long countByWorkspaceIdAndHealthStatus(Long workspaceId, String healthStatus);
}
