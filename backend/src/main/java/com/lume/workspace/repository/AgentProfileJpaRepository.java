package com.lume.workspace.repository;

import com.lume.workspace.entity.AgentProfileJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AgentProfileJpaRepository extends JpaRepository<AgentProfileJpaEntity, String> {

    List<AgentProfileJpaEntity> findByWorkspaceIdOrderByNameAsc(Long workspaceId);

    Optional<AgentProfileJpaEntity> findByIdAndWorkspaceId(String id, Long workspaceId);
}
