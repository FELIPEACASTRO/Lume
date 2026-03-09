package com.lume.workspace.dto;

import java.util.List;

public record ShellNavigationItemResponse(
        String id,
        String label,
        String path,
        String description,
        String icon,
        String availability,
        String group,
        List<String> keywords
) {
}
