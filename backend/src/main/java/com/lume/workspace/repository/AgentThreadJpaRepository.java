package com.lume.workspace.repository;

import com.lume.workspace.entity.AgentThreadJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AgentThreadJpaRepository extends JpaRepository<AgentThreadJpaEntity, String> {

    List<AgentThreadJpaEntity> findByWorkspaceIdOrderByUpdatedAtDesc(Long workspaceId);

    long countByWorkspaceId(Long workspaceId);

    Optional<AgentThreadJpaEntity> findByIdAndWorkspaceId(String id, Long workspaceId);
}
