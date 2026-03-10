package com.lume.workspace.dto;

import java.util.List;

public record HomeOverviewResponse(
        String workspaceName,
        String organizationName,
        String headline,
        String supportingText,
        java.util.List<HomeOverviewBlockResponse> blocks,
        List<HomeFocusItemResponse> inProgress,
        List<RecentItemResponse> recentItems,
        List<HomeAlertResponse> alerts,
        List<WorkspaceFacetResponse> teamAndContext
) {
}
