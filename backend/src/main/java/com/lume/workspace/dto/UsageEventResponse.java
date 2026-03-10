package com.lume.workspace.dto;

public record UsageEventResponse(
        Long id,
        String eventType,
        String resourceType,
        String resourceId,
        Long actorUserId,
        String details,
        String createdAt
) {
}
