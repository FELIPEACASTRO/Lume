package com.lume.workspace.repository;

import com.lume.workspace.entity.NotificationJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationJpaRepository extends JpaRepository<NotificationJpaEntity, String> {

    List<NotificationJpaEntity> findByWorkspaceIdOrderByCreatedAtDesc(Long workspaceId);

    List<NotificationJpaEntity> findByWorkspaceIdOrderByCreatedAtDesc(Long workspaceId, Pageable pageable);

    List<NotificationJpaEntity> findByWorkspaceIdAndReadFalseOrderByCreatedAtDesc(Long workspaceId, Pageable pageable);

    long countByWorkspaceIdAndReadFalse(Long workspaceId);
}
