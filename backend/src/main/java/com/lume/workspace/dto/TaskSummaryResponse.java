package com.lume.workspace.dto;

public record TaskSummaryResponse(
        String id,
        String projectId,
        String projectName,
        String taskType,
        String title,
        String prompt,
        String summary,
        String statusLabel,
        String availability,
        String runtimeState,
        String lastError,
        String ownerName,
        String updatedAt,
        String scheduledFor,
        String shareSlug
) {
}
