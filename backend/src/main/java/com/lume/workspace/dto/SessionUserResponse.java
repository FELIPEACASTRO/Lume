package com.lume.workspace.dto;

public record SessionUserResponse(
        Long id,
        String name,
        String email,
        String initials
) {
}
