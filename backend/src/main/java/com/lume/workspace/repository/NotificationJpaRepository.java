package com.lume.workspace.repository;

import com.lume.workspace.entity.NotificationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationJpaRepository extends JpaRepository<NotificationJpaEntity, String> {

    List<NotificationJpaEntity> findByWorkspaceIdOrderByCreatedAtDesc(Long workspaceId);

    long countByWorkspaceIdAndReadFalse(Long workspaceId);
}
