package com.lume.workspace.dto;

public record HomeOverviewBlockResponse(
        String id,
        String blockType,
        String title,
        String description,
        int sortOrder,
        int maxItems,
        String ctaLabel,
        String ctaPath,
        boolean enabled
) {
}
