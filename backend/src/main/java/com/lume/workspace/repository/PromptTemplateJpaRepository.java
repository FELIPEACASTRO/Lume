package com.lume.workspace.repository;

import com.lume.workspace.entity.PromptTemplateJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PromptTemplateJpaRepository extends JpaRepository<PromptTemplateJpaEntity, String> {

    List<PromptTemplateJpaEntity> findByWorkspaceIdOrderByUpdatedAtDesc(Long workspaceId);

    long countByWorkspaceId(Long workspaceId);

    Optional<PromptTemplateJpaEntity> findByIdAndWorkspaceId(String id, Long workspaceId);
}
