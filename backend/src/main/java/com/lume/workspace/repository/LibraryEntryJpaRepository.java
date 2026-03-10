package com.lume.workspace.repository;

import com.lume.workspace.entity.LibraryEntryJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LibraryEntryJpaRepository extends JpaRepository<LibraryEntryJpaEntity, String> {

    List<LibraryEntryJpaEntity> findByWorkspaceIdOrderByUpdatedAtDesc(Long workspaceId);

    List<LibraryEntryJpaEntity> findByWorkspaceIdOrderByUpdatedAtDesc(Long workspaceId, Pageable pageable);

    long countByWorkspaceId(Long workspaceId);

    @Query("""
            select e
            from LibraryEntryJpaEntity e
            where e.workspaceId = :workspaceId
              and (
                    lower(e.title) like lower(concat('%', :query, '%'))
                 or lower(e.category) like lower(concat('%', :query, '%'))
                 or lower(e.ownerName) like lower(concat('%', :query, '%'))
                 or lower(e.sourceLabel) like lower(concat('%', :query, '%'))
                 or lower(e.summary) like lower(concat('%', :query, '%'))
              )
            order by e.updatedAt desc
            """)
    List<LibraryEntryJpaEntity> searchByWorkspaceId(
            @Param("workspaceId") Long workspaceId,
            @Param("query") String query,
            Pageable pageable
    );

    Optional<LibraryEntryJpaEntity> findByIdAndWorkspaceId(String id, Long workspaceId);
}
