package com.lume.workspace.dto;

import java.util.List;

public record PromptTemplateResponse(
        String id,
        String title,
        String summary,
        String promptBody,
        List<String> variables,
        String templateScope,
        String statusLabel,
        String availability,
        String ownerName,
        String projectId,
        String projectName,
        String agentProfileId,
        String agentProfileName,
        boolean favorited,
        String lastUsedAt,
        String updatedAt
) {
}
