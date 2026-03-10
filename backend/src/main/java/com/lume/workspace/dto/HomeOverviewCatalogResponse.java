package com.lume.workspace.dto;

import java.util.List;

public record HomeOverviewCatalogResponse(
        HomeOverviewSettingsResponse settings,
        List<HomeOverviewBlockResponse> blocks
) {
}
