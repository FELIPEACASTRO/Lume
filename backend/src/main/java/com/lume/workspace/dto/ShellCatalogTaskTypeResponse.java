package com.lume.workspace.dto;

public record ShellCatalogTaskTypeResponse(
        String taskType,
        String label,
        String description,
        int sortOrder,
        boolean enabled
) {
}
