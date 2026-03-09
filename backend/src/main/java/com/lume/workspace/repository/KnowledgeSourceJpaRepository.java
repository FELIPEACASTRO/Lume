package com.lume.workspace.repository;

import com.lume.workspace.entity.KnowledgeSourceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KnowledgeSourceJpaRepository extends JpaRepository<KnowledgeSourceJpaEntity, String> {

    List<KnowledgeSourceJpaEntity> findByWorkspaceIdOrderByUpdatedAtDesc(Long workspaceId);

    List<KnowledgeSourceJpaEntity> findByWorkspaceIdAndProjectIdOrderByUpdatedAtDesc(Long workspaceId, String projectId);

    Optional<KnowledgeSourceJpaEntity> findByIdAndWorkspaceId(String id, Long workspaceId);

    long countByWorkspaceId(Long workspaceId);
}
