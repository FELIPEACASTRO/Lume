package com.lume.workspace.dto;

public record UsageSummaryResponse(
        int dailyCredits,
        int consumedCredits,
        int remainingCredits,
        int activeTasks,
        int scheduledTasks,
        int unreadNotifications,
        String note,
        BudgetSummaryResponse budget,
        WorkspaceCommercialSummaryResponse commercial
) {
}
