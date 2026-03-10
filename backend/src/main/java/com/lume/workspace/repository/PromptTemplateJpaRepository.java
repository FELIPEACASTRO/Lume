package com.lume.workspace.repository;

import com.lume.workspace.entity.PromptTemplateJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PromptTemplateJpaRepository extends JpaRepository<PromptTemplateJpaEntity, String> {

    List<PromptTemplateJpaEntity> findByWorkspaceIdOrderByUpdatedAtDesc(Long workspaceId);

    List<PromptTemplateJpaEntity> findByWorkspaceIdOrderByUpdatedAtDesc(Long workspaceId, Pageable pageable);

    long countByWorkspaceId(Long workspaceId);

    @Query("""
            select p
            from PromptTemplateJpaEntity p
            where p.workspaceId = :workspaceId
              and (
                    lower(p.title) like lower(concat('%', :query, '%'))
                 or lower(p.summary) like lower(concat('%', :query, '%'))
                 or lower(p.promptBody) like lower(concat('%', :query, '%'))
                 or lower(p.variablesRaw) like lower(concat('%', :query, '%'))
              )
            order by p.updatedAt desc
            """)
    List<PromptTemplateJpaEntity> searchByWorkspaceId(
            @Param("workspaceId") Long workspaceId,
            @Param("query") String query,
            Pageable pageable
    );

    Optional<PromptTemplateJpaEntity> findByIdAndWorkspaceId(String id, Long workspaceId);
}
