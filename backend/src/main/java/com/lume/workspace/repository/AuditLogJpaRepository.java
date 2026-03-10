package com.lume.workspace.repository;

import com.lume.workspace.entity.AuditLogJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogJpaRepository extends JpaRepository<AuditLogJpaEntity, Long> {
}
