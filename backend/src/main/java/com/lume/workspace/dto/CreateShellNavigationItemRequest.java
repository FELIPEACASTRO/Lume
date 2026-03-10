package com.lume.workspace.dto;

import java.util.List;

public record CreateShellNavigationItemRequest(
        String id,
        String label,
        String path,
        String description,
        String icon,
        String availability,
        String group,
        Integer sortOrder,
        Boolean enabled,
        List<String> keywords
) {
}
