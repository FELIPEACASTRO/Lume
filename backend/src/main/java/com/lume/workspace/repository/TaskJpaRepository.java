package com.lume.workspace.repository;

import com.lume.workspace.entity.TaskJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskJpaRepository extends JpaRepository<TaskJpaEntity, String> {

    List<TaskJpaEntity> findByWorkspaceIdOrderByUpdatedAtDesc(Long workspaceId);

    List<TaskJpaEntity> findByWorkspaceIdAndProjectIdOrderByUpdatedAtDesc(Long workspaceId, String projectId);

    Optional<TaskJpaEntity> findByIdAndWorkspaceId(String id, Long workspaceId);
}
