package com.lume.workspace.repository;

import com.lume.workspace.entity.AgentThreadJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AgentThreadJpaRepository extends JpaRepository<AgentThreadJpaEntity, String> {

    List<AgentThreadJpaEntity> findByWorkspaceIdOrderByUpdatedAtDesc(Long workspaceId);

    List<AgentThreadJpaEntity> findByWorkspaceIdOrderByUpdatedAtDesc(Long workspaceId, Pageable pageable);

    long countByWorkspaceId(Long workspaceId);

    @Query("""
            select t
            from AgentThreadJpaEntity t
            where t.workspaceId = :workspaceId
              and (
                    lower(t.title) like lower(concat('%', :query, '%'))
                 or lower(coalesce(t.lastMessagePreview, '')) like lower(concat('%', :query, '%'))
              )
            order by t.updatedAt desc
            """)
    List<AgentThreadJpaEntity> searchByWorkspaceId(
            @Param("workspaceId") Long workspaceId,
            @Param("query") String query,
            Pageable pageable
    );

    Optional<AgentThreadJpaEntity> findByIdAndWorkspaceId(String id, Long workspaceId);
}
