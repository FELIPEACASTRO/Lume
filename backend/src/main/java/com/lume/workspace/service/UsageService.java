package com.lume.workspace.service;

import com.lume.workspace.dto.UsageSummaryResponse;
import org.springframework.stereotype.Service;

@Service
public class UsageService {

    private final WorkspaceMeteringService workspaceMeteringService;
    private final WorkspaceBudgetService workspaceBudgetService;
    private final WorkspaceCommercialService workspaceCommercialService;

    public UsageService(
            WorkspaceMeteringService workspaceMeteringService,
            WorkspaceBudgetService workspaceBudgetService,
            WorkspaceCommercialService workspaceCommercialService
    ) {
        this.workspaceMeteringService = workspaceMeteringService;
        this.workspaceBudgetService = workspaceBudgetService;
        this.workspaceCommercialService = workspaceCommercialService;
    }

    public UsageSummaryResponse getSummary() {
        WorkspaceMeteringService.UsageMeteringSnapshot snapshot = workspaceMeteringService.currentSnapshot();

        return new UsageSummaryResponse(
                snapshot.dailyCredits(),
                snapshot.consumedCredits(),
                snapshot.remainingCredits(),
                snapshot.activeTasks(),
                snapshot.scheduledTasks(),
                snapshot.unreadNotifications(),
                "Uso operacional (runtime) separado do faturamento comercial. Consulte /api/v1/finops e /api/v1/billing para ledger e cobranca.",
                workspaceBudgetService.summarizeForConsumedCredits(snapshot.consumedCredits()),
                workspaceCommercialService.getCurrentSummary()
        );
    }
}
