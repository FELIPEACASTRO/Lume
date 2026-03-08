package com.lume.workspace.repository;

import com.lume.workspace.entity.MembershipJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MembershipJpaRepository extends JpaRepository<MembershipJpaEntity, Long> {

    List<MembershipJpaEntity> findByUserIdAndActiveTrueOrderByCreatedAtAsc(Long userId);

    Optional<MembershipJpaEntity> findByUserIdAndWorkspaceId(Long userId, Long workspaceId);

    Optional<MembershipJpaEntity> findByUserIdAndWorkspaceIdAndActiveTrue(Long userId, Long workspaceId);

    List<MembershipJpaEntity> findByWorkspaceIdAndActiveTrueOrderByCreatedAtAsc(Long workspaceId);

    long countByWorkspaceIdAndActiveTrue(Long workspaceId);
}
