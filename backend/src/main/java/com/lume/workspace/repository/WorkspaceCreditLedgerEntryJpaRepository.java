package com.lume.workspace.repository;

import com.lume.workspace.entity.WorkspaceCreditLedgerEntryJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface WorkspaceCreditLedgerEntryJpaRepository extends JpaRepository<WorkspaceCreditLedgerEntryJpaEntity, Long> {

    List<WorkspaceCreditLedgerEntryJpaEntity> findByWorkspaceIdOrderByCreatedAtDesc(Long workspaceId, Pageable pageable);

    @Query("select coalesce(sum(e.creditsDelta), 0) from WorkspaceCreditLedgerEntryJpaEntity e where e.workspaceId = :workspaceId")
    int sumCreditsByWorkspaceId(Long workspaceId);
}
