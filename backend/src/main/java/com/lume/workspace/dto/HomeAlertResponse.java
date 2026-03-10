package com.lume.workspace.dto;

public record HomeAlertResponse(
        String id,
        String title,
        String body,
        String kind,
        String path,
        String createdAt,
        boolean read
) {
}
