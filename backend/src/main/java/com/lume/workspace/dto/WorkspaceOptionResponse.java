package com.lume.workspace.dto;

public record WorkspaceOptionResponse(
        Long id,
        String name,
        String slug,
        Long organizationId,
        String organizationName,
        String roleCode,
        String roleLabel,
        boolean active
) {
}
