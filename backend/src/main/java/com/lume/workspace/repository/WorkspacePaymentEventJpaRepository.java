package com.lume.workspace.repository;

import com.lume.workspace.entity.WorkspacePaymentEventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface WorkspacePaymentEventJpaRepository extends JpaRepository<WorkspacePaymentEventJpaEntity, Long> {

    List<WorkspacePaymentEventJpaEntity> findByWorkspaceIdOrderByOccurredAtDesc(Long workspaceId);

    Optional<WorkspacePaymentEventJpaEntity> findByWorkspaceIdAndGatewayEventId(Long workspaceId, String gatewayEventId);

    boolean existsByWorkspaceIdAndGatewayEventId(Long workspaceId, String gatewayEventId);

    long countByWorkspaceId(Long workspaceId);

    long countByWorkspaceIdAndStatus(Long workspaceId, String status);

    long countByWorkspaceIdAndInvoiceIsNull(Long workspaceId);

    long countByWorkspaceIdAndStatusAndProcessedAtIsNull(Long workspaceId, String status);

    @Query("select coalesce(sum(e.amountBrl), 0) from WorkspacePaymentEventJpaEntity e where e.workspaceId = :workspaceId and e.status = :status")
    BigDecimal sumAmountByWorkspaceIdAndStatus(Long workspaceId, String status);
}
