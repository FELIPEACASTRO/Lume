package com.lume.workspace.service;

import com.lume.domain.exception.BusinessRuleException;
import com.lume.workspace.dto.BudgetSummaryResponse;
import com.lume.workspace.dto.UpdateWorkspaceBudgetRequest;
import com.lume.workspace.entity.WorkspaceBudgetJpaEntity;
import com.lume.workspace.repository.WorkspaceBudgetJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class WorkspaceBudgetService {

    private final WorkspaceBudgetJpaRepository workspaceBudgetRepository;
    private final WorkspaceMeteringService workspaceMeteringService;
    private final WorkspaceContextService workspaceContextService;
    private final AuditLogService auditLogService;

    public WorkspaceBudgetService(
            WorkspaceBudgetJpaRepository workspaceBudgetRepository,
            WorkspaceMeteringService workspaceMeteringService,
            WorkspaceContextService workspaceContextService,
            AuditLogService auditLogService
    ) {
        this.workspaceBudgetRepository = workspaceBudgetRepository;
        this.workspaceMeteringService = workspaceMeteringService;
        this.workspaceContextService = workspaceContextService;
        this.auditLogService = auditLogService;
    }

    public BudgetSummaryResponse getCurrentBudget() {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_BUDGETS_READ);
        return summarizeForConsumedCredits(workspaceMeteringService.currentSnapshot().consumedCredits());
    }

    @Transactional
    public BudgetSummaryResponse updateCurrentBudget(UpdateWorkspaceBudgetRequest request) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_BUDGETS_MANAGE);
        WorkspaceBudgetJpaEntity entity = getOrCreateBudget();

        if (request.costCenter() != null && !request.costCenter().isBlank()) {
            entity.setCostCenter(request.costCenter().trim());
        }
        if (request.chargebackMode() != null) {
            entity.setChargebackMode(request.chargebackMode());
        }
        if (request.softLimitCredits() != null) {
            entity.setSoftLimitCredits(request.softLimitCredits());
        }
        if (request.hardLimitCredits() != null) {
            entity.setHardLimitCredits(request.hardLimitCredits());
        }

        validateBudget(entity);
        WorkspaceBudgetJpaEntity saved = workspaceBudgetRepository.save(entity);
        auditLogService.record(
                "workspace_budget",
                String.valueOf(saved.getWorkspaceId()),
                "updated",
                Map.of(
                        "costCenter", saved.getCostCenter(),
                        "chargebackMode", saved.getChargebackMode(),
                        "softLimitCredits", saved.getSoftLimitCredits(),
                        "hardLimitCredits", saved.getHardLimitCredits()
                )
        );
        return summarize(saved, workspaceMeteringService.currentSnapshot().consumedCredits());
    }

    public BudgetSummaryResponse summarizeForConsumedCredits(int consumedCredits) {
        return summarize(getOrCreateBudget(), consumedCredits);
    }

    private WorkspaceBudgetJpaEntity getOrCreateBudget() {
        return workspaceBudgetRepository.findByWorkspaceId(workspaceContextService.getWorkspaceId()).orElseGet(() -> {
            WorkspaceBudgetJpaEntity entity = new WorkspaceBudgetJpaEntity();
            entity.setWorkspaceId(workspaceContextService.getWorkspaceId());
            return workspaceBudgetRepository.save(entity);
        });
    }

    private BudgetSummaryResponse summarize(WorkspaceBudgetJpaEntity entity, int consumedCredits) {
        int remainingSoft = Math.max(0, entity.getSoftLimitCredits() - consumedCredits);
        int remainingHard = Math.max(0, entity.getHardLimitCredits() - consumedCredits);
        boolean softLimitReached = consumedCredits >= entity.getSoftLimitCredits();
        boolean hardLimitReached = consumedCredits >= entity.getHardLimitCredits();
        String budgetStatus = hardLimitReached ? "hard_limit_reached" : softLimitReached ? "soft_limit_reached" : "healthy";

        return new BudgetSummaryResponse(
                entity.getCostCenter(),
                entity.getChargebackMode(),
                entity.getSoftLimitCredits(),
                entity.getHardLimitCredits(),
                consumedCredits,
                remainingSoft,
                remainingHard,
                utilizationPercent(consumedCredits, entity.getSoftLimitCredits()),
                utilizationPercent(consumedCredits, entity.getHardLimitCredits()),
                softLimitReached,
                hardLimitReached,
                budgetStatus,
                "Budget operacional por workspace. Custos comerciais finais e reconciliacao ficam em uma trilha posterior de billing."
        );
    }

    private int utilizationPercent(int numerator, int denominator) {
        if (denominator <= 0) {
            return 100;
        }
        return Math.min(999, (int) Math.round((numerator * 100.0) / denominator));
    }

    private void validateBudget(WorkspaceBudgetJpaEntity entity) {
        if (entity.getSoftLimitCredits() < 0 || entity.getHardLimitCredits() < 0) {
            throw new BusinessRuleException("Os limites de budget nao podem ser negativos.");
        }
        if (entity.getHardLimitCredits() < entity.getSoftLimitCredits()) {
            throw new BusinessRuleException("O hard limit precisa ser maior ou igual ao soft limit.");
        }
        if (!"showback".equals(entity.getChargebackMode()) && !"chargeback".equals(entity.getChargebackMode())) {
            throw new BusinessRuleException("chargebackMode deve ser showback ou chargeback.");
        }
    }
}
