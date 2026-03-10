package com.lume.workspace.dto;

import java.util.List;

public record SessionRoleResponse(
        String code,
        String label,
        List<String> permissions
) {
}
