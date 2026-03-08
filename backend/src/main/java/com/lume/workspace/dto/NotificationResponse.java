package com.lume.workspace.dto;

public record NotificationResponse(
        String id,
        String kind,
        String title,
        String body,
        String path,
        boolean read,
        String createdAt
) {
}
