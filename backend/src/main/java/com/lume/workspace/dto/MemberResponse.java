package com.lume.workspace.dto;

public record MemberResponse(
        Long id,
        Long userId,
        String name,
        String email,
        boolean active,
        String roleCode,
        String roleLabel,
        String createdAt,
        boolean currentUser
) {
}
