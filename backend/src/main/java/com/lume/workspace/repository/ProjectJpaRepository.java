package com.lume.workspace.repository;

import com.lume.workspace.entity.ProjectJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectJpaRepository extends JpaRepository<ProjectJpaEntity, String> {

    List<ProjectJpaEntity> findByWorkspaceIdOrderByUpdatedAtDesc(Long workspaceId);

    List<ProjectJpaEntity> findByWorkspaceIdOrderByUpdatedAtDesc(Long workspaceId, Pageable pageable);

    long countByWorkspaceId(Long workspaceId);

    @Query("""
            select p
            from ProjectJpaEntity p
            where p.workspaceId = :workspaceId
              and (
                    lower(p.name) like lower(concat('%', :query, '%'))
                 or lower(p.summary) like lower(concat('%', :query, '%'))
                 or lower(p.ownerName) like lower(concat('%', :query, '%'))
              )
            order by p.updatedAt desc
            """)
    List<ProjectJpaEntity> searchByWorkspaceId(
            @Param("workspaceId") Long workspaceId,
            @Param("query") String query,
            Pageable pageable
    );

    Optional<ProjectJpaEntity> findByIdAndWorkspaceId(String id, Long workspaceId);
}
