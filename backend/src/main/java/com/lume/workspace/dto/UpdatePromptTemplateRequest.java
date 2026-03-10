package com.lume.workspace.dto;

import java.util.List;

public record UpdatePromptTemplateRequest(
        String title,
        String summary,
        String promptBody,
        String templateScope,
        String projectId,
        String agentProfileId,
        List<String> variables,
        Boolean favorited,
        String statusLabel,
        String availability
) {
}
