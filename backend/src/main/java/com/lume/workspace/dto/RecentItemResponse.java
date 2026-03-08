package com.lume.workspace.dto;

public record RecentItemResponse(
        String id,
        String title,
        String summary,
        String detail,
        String path,
        String availability
) {
}
