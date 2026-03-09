package com.lume.workspace.dto;

import java.util.List;

public record ShellNavigationResponse(
        List<ShellNavigationItemResponse> items,
        List<ShellTaskTypeResponse> taskTypes
) {
}
