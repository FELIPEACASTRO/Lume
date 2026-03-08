package com.lume.workspace.repository;

import com.lume.workspace.entity.KnowledgeSourceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KnowledgeSourceJpaRepository extends JpaRepository<KnowledgeSourceJpaEntity, String> {

    List<KnowledgeSourceJpaEntity> findByWorkspaceIdOrderByUpdatedAtDesc(Long workspaceId);
}
