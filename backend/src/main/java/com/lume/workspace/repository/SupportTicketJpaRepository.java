package com.lume.workspace.repository;

import com.lume.workspace.entity.SupportTicketJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupportTicketJpaRepository extends JpaRepository<SupportTicketJpaEntity, String> {

    List<SupportTicketJpaEntity> findByWorkspaceIdOrderByUpdatedAtDesc(Long workspaceId);

    List<SupportTicketJpaEntity> findByWorkspaceIdOrderByUpdatedAtDesc(Long workspaceId, Pageable pageable);

    Optional<SupportTicketJpaEntity> findByIdAndWorkspaceId(String id, Long workspaceId);

    List<SupportTicketJpaEntity> findByWorkspaceIdAndStatusIn(Long workspaceId, List<String> statuses);

    long countByWorkspaceIdAndStatusIn(Long workspaceId, List<String> statuses);

    long countByWorkspaceIdAndStatusInAndSeverity(Long workspaceId, List<String> statuses, String severity);
}
