package com.lume.workspace.repository;

import com.lume.workspace.entity.ProjectJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectJpaRepository extends JpaRepository<ProjectJpaEntity, String> {

    List<ProjectJpaEntity> findByWorkspaceIdOrderByUpdatedAtDesc(Long workspaceId);
}
