package com.lume.workspace.service;

import com.lume.workspace.dto.UsageSummaryResponse;
import org.springframework.stereotype.Service;

@Service
public class UsageService {

    private final WorkspaceMeteringService workspaceMeteringService;
    private final WorkspaceBudgetService workspaceBudgetService;

    public UsageService(
            WorkspaceMeteringService workspaceMeteringService,
            WorkspaceBudgetService workspaceBudgetService
    ) {
        this.workspaceMeteringService = workspaceMeteringService;
        this.workspaceBudgetService = workspaceBudgetService;
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
                "A camada de creditos aqui representa metering operacional do workspace, nao faturamento final.",
                workspaceBudgetService.summarizeForConsumedCredits(snapshot.consumedCredits())
        );
    }
}
