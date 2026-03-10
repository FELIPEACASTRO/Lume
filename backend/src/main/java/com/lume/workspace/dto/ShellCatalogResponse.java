package com.lume.workspace.dto;

import java.util.List;

public record ShellCatalogResponse(
        List<ShellCatalogItemResponse> items,
        List<ShellCatalogTaskTypeResponse> taskTypes
) {
}
