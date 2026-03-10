package com.lume.workspace.repository;

import com.lume.workspace.entity.AgentProfileJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AgentProfileJpaRepository extends JpaRepository<AgentProfileJpaEntity, String> {

    List<AgentProfileJpaEntity> findByWorkspaceIdOrderByNameAsc(Long workspaceId);

    List<AgentProfileJpaEntity> findByWorkspaceIdOrderByNameAsc(Long workspaceId, Pageable pageable);

    @Query("""
            select a
            from AgentProfileJpaEntity a
            where a.workspaceId = :workspaceId
              and (
                    lower(a.name) like lower(concat('%', :query, '%'))
                 or lower(a.specialty) like lower(concat('%', :query, '%'))
                 or lower(a.description) like lower(concat('%', :query, '%'))
                 or lower(a.note) like lower(concat('%', :query, '%'))
              )
            order by a.name asc
            """)
    List<AgentProfileJpaEntity> searchByWorkspaceId(
            @Param("workspaceId") Long workspaceId,
            @Param("query") String query,
            Pageable pageable
    );

    Optional<AgentProfileJpaEntity> findByIdAndWorkspaceId(String id, Long workspaceId);
}
