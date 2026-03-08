package com.lume.workspace.repository;

import com.lume.workspace.entity.LibraryEntryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LibraryEntryJpaRepository extends JpaRepository<LibraryEntryJpaEntity, String> {

    List<LibraryEntryJpaEntity> findByWorkspaceIdOrderByUpdatedAtDesc(Long workspaceId);

    Optional<LibraryEntryJpaEntity> findByIdAndWorkspaceId(String id, Long workspaceId);
}
