package com.lume.workspace.dto;

public record UpdateShellTaskTypeRequest(
        String label,
        String description,
        Integer sortOrder,
        Boolean enabled
) {
}
