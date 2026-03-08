package com.lume.workspace.repository;

import com.lume.workspace.entity.WorkspaceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkspaceJpaRepository extends JpaRepository<WorkspaceJpaEntity, Long> {

    Optional<WorkspaceJpaEntity> findBySlug(String slug);

    List<WorkspaceJpaEntity> findByOrganizationIdOrderByNameAsc(Long organizationId);
}
