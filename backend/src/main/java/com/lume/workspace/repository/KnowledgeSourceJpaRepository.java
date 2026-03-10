package com.lume.workspace.repository;

import com.lume.workspace.entity.KnowledgeSourceJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface KnowledgeSourceJpaRepository extends JpaRepository<KnowledgeSourceJpaEntity, String> {

    List<KnowledgeSourceJpaEntity> findByWorkspaceIdOrderByUpdatedAtDesc(Long workspaceId);

    List<KnowledgeSourceJpaEntity> findByWorkspaceIdOrderByUpdatedAtDesc(Long workspaceId, Pageable pageable);

    List<KnowledgeSourceJpaEntity> findTop5ByWorkspaceIdOrderByUpdatedAtDesc(Long workspaceId);

    List<KnowledgeSourceJpaEntity> findByWorkspaceIdAndProjectIdOrderByUpdatedAtDesc(Long workspaceId, String projectId);

    Optional<KnowledgeSourceJpaEntity> findByIdAndWorkspaceId(String id, Long workspaceId);

    long countByWorkspaceId(Long workspaceId);

    @Query("""
            select k
            from KnowledgeSourceJpaEntity k
            where k.workspaceId = :workspaceId
              and (
                    lower(k.title) like lower(concat('%', :query, '%'))
                 or lower(k.sourceType) like lower(concat('%', :query, '%'))
                 or lower(coalesce(k.sourceUri, '')) like lower(concat('%', :query, '%'))
                 or lower(k.note) like lower(concat('%', :query, '%'))
              )
            order by k.updatedAt desc
            """)
    List<KnowledgeSourceJpaEntity> searchByWorkspaceId(
            @Param("workspaceId") Long workspaceId,
            @Param("query") String query,
            Pageable pageable
    );
}
