package com.lume.workspace.dto;

public record ProjectResponse(
        String id,
        String name,
        String summary,
        String statusLabel,
        String availability,
        String ownerName,
        int taskCount,
        String updatedAt
) {
}
