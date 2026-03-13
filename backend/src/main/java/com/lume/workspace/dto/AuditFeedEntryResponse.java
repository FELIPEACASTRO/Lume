package com.lume.workspace.dto;

public record AuditFeedEntryResponse(
        Long id,
        Long actorUserId,
        String entityType,
        String entityId,
        String action,
        String payload,
        String createdAt
) {
}
