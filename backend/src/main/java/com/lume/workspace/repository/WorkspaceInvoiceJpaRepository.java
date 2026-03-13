package com.lume.workspace.repository;

import com.lume.workspace.entity.WorkspaceInvoiceJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface WorkspaceInvoiceJpaRepository extends JpaRepository<WorkspaceInvoiceJpaEntity, Long> {

    List<WorkspaceInvoiceJpaEntity> findByWorkspaceIdOrderByCreatedAtDesc(Long workspaceId);

    List<WorkspaceInvoiceJpaEntity> findByWorkspaceIdOrderByCreatedAtDesc(Long workspaceId, Pageable pageable);

    Optional<WorkspaceInvoiceJpaEntity> findByInvoiceNumber(String invoiceNumber);

    long countByWorkspaceId(Long workspaceId);

    long countByWorkspaceIdAndStatus(Long workspaceId, String status);

    @Query("select coalesce(sum(i.amountBrl), 0) from WorkspaceInvoiceJpaEntity i where i.workspaceId = :workspaceId and i.status = :status")
    BigDecimal sumAmountByWorkspaceIdAndStatus(Long workspaceId, String status);
}
