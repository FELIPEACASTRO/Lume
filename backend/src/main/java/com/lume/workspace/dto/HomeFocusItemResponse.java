package com.lume.workspace.dto;

public record HomeFocusItemResponse(
        String id,
        String title,
        String summary,
        String statusLabel,
        String runtimeState,
        String ownerName,
        String path
) {
}
