package com.lume.workspace.dto;

public record WorkspaceFacetResponse(
        String id,
        String label,
        String headline,
        String description,
        String availability,
        String path
) {
}
