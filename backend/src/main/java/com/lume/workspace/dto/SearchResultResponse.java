package com.lume.workspace.dto;

import java.util.List;

public record SearchResultResponse(
        String id,
        String title,
        String description,
        String path,
        String section,
        String availability,
        List<String> keywords
) {
}
