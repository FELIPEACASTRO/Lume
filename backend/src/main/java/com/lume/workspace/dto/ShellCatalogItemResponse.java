package com.lume.workspace.dto;

import java.util.List;

public record ShellCatalogItemResponse(
        String id,
        String label,
        String path,
        String description,
        String icon,
        String availability,
        String group,
        int sortOrder,
        boolean enabled,
        List<String> keywords
) {
}
