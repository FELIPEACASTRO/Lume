package com.lume.workspace.dto;

public record ResearchResultItemResponse(
        String title,
        String url,
        String snippet,
        String source,
        Double score
) {
}
