package com.lume.workspace.dto;

public record ArtifactVersionResponse(
        String id,
        String entryId,
        String versionLabel,
        String changeSummary,
        String contentPreview,
        String createdByName,
        String createdAt
) {
}
