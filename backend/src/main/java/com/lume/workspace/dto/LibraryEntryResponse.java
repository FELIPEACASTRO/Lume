package com.lume.workspace.dto;

import java.util.List;

public record LibraryEntryResponse(
        String id,
        String title,
        String category,
        String entryType,
        String status,
        String availability,
        String owner,
        String sourceLabel,
        String summary,
        List<String> tags,
        String projectId,
        String projectName,
        boolean favorited,
        boolean archived,
        int versionCount,
        String currentVersionLabel
) {
}
