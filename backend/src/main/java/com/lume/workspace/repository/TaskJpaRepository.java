package com.lume.workspace.repository;

import com.lume.workspace.entity.TaskJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TaskJpaRepository extends JpaRepository<TaskJpaEntity, String> {

    List<TaskJpaEntity> findByWorkspaceIdOrderByUpdatedAtDesc(Long workspaceId);

    List<TaskJpaEntity> findByWorkspaceIdOrderByUpdatedAtDesc(Long workspaceId, Pageable pageable);

    long countByWorkspaceId(Long workspaceId);

    long countByWorkspaceIdAndRuntimeState(Long workspaceId, String runtimeState);

    long countByWorkspaceIdAndScheduledForIsNotNull(Long workspaceId);

    List<TaskJpaEntity> findByWorkspaceIdAndProjectIdOrderByUpdatedAtDesc(Long workspaceId, String projectId);

    List<TaskJpaEntity> findByWorkspaceIdAndRuntimeStateInOrderByUpdatedAtDesc(
            Long workspaceId,
            List<String> runtimeStates,
            Pageable pageable
    );

    @Query("""
            select t
            from TaskJpaEntity t
            where t.workspaceId = :workspaceId
              and (
                    lower(t.title) like lower(concat('%', :query, '%'))
                 or lower(t.prompt) like lower(concat('%', :query, '%'))
                 or lower(t.summary) like lower(concat('%', :query, '%'))
                 or lower(t.taskType) like lower(concat('%', :query, '%'))
              )
            order by t.updatedAt desc
            """)
    List<TaskJpaEntity> searchByWorkspaceId(
            @Param("workspaceId") Long workspaceId,
            @Param("query") String query,
            Pageable pageable
    );

    Optional<TaskJpaEntity> findByIdAndWorkspaceId(String id, Long workspaceId);

    Optional<TaskJpaEntity> findFirstByWorkspaceIdOrderByCreatedAtAsc(Long workspaceId);
}
