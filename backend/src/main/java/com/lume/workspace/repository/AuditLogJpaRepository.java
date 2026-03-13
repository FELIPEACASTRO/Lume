package com.lume.workspace.repository;

import com.lume.workspace.entity.AuditLogJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogJpaRepository extends JpaRepository<AuditLogJpaEntity, Long> {
    List<AuditLogJpaEntity> findByWorkspaceIdOrderByCreatedAtDesc(Long workspaceId, Pageable pageable);
}
