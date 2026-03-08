package com.lume.workspace.dto;

public record SummaryCountsResponse(
        int users,
        int libraryEntries,
        int agentThreads,
        int projects,
        int tasks,
        int unreadNotifications
) {
}
