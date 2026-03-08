package com.lume.workspace.dto;

import java.util.List;

public record WorkspaceSummaryResponse(
        String workspaceName,
        String organizationName,
        SummaryCountsResponse counts,
        List<RecentItemResponse> recentItems,
        List<WorkspaceFacetResponse> workspaceFacets
) {
}
