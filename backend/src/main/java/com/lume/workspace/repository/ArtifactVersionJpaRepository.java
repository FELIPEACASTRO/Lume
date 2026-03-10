package com.lume.workspace.repository;

import com.lume.workspace.entity.ArtifactVersionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArtifactVersionJpaRepository extends JpaRepository<ArtifactVersionJpaEntity, String> {

    List<ArtifactVersionJpaEntity> findByWorkspaceIdOrderByCreatedAtDesc(Long workspaceId);

    List<ArtifactVersionJpaEntity> findByEntryIdAndWorkspaceIdOrderByCreatedAtDesc(String entryId, Long workspaceId);

    long countByEntryIdAndWorkspaceId(String entryId, Long workspaceId);
}
