package com.lume.workspace.repository;

import com.lume.workspace.entity.ProjectJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectJpaRepository extends JpaRepository<ProjectJpaEntity, String> {

    List<ProjectJpaEntity> findByWorkspaceIdOrderByUpdatedAtDesc(Long workspaceId);

    long countByWorkspaceId(Long workspaceId);

    Optional<ProjectJpaEntity> findByIdAndWorkspaceId(String id, Long workspaceId);
}
