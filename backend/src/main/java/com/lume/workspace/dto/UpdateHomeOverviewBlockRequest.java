package com.lume.workspace.dto;

public record UpdateHomeOverviewBlockRequest(
        String title,
        String description,
        Integer sortOrder,
        Integer maxItems,
        String ctaLabel,
        String ctaPath,
        Boolean enabled
) {
}
