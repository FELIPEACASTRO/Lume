package com.lume.workspace.dto;

import java.util.List;

public record SearchResultsResponse(
        String query,
        int totalResults,
        List<SearchResultResponse> results
) {
}
