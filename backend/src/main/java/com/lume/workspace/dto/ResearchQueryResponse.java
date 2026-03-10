package com.lume.workspace.dto;

import java.util.List;

public record ResearchQueryResponse(
        String providerCode,
        String providerName,
        boolean configured,
        boolean executionSupported,
        String status,
        String query,
        List<ResearchResultItemResponse> items,
        String error
) {
}
