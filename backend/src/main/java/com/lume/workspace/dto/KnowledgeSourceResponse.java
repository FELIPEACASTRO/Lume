package com.lume.workspace.dto;

public record KnowledgeSourceResponse(
        String id,
        String title,
        String sourceType,
        String sourceUri,
        String projectId,
        String projectName,
        String statusLabel,
        String availability,
        int documentCount,
        boolean enabledForAgents,
        String note,
        String lastIndexedAt,
        String updatedAt
) {
}
