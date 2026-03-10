package com.lume.workspace.dto;

public record CreateHomeOverviewBlockRequest(
        String id,
        String blockType,
        String title,
        String description,
        Integer sortOrder,
        Integer maxItems,
        String ctaLabel,
        String ctaPath,
        Boolean enabled
) {
}
