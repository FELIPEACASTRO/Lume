package com.lume.workspace.dto;

public record UpdateMemberRequest(
        String name,
        String email,
        String password,
        String roleCode,
        Boolean active
) {
}
