package com.lume.workspace.dto;

public record CreateShellTaskTypeRequest(
        String taskType,
        String label,
        String description,
        Integer sortOrder,
        Boolean enabled
) {
}
