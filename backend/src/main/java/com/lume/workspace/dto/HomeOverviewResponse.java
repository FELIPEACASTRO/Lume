package com.lume.workspace.dto;

import java.util.List;

public record HomeOverviewResponse(
        String workspaceName,
        String organizationName,
        String headline,
        String supportingText,
        List<HomeFocusItemResponse> inProgress,
        List<RecentItemResponse> recentItems,
        List<HomeAlertResponse> alerts,
        List<WorkspaceFacetResponse> teamAndContext
) {
}
