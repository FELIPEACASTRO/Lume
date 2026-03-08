package com.lume.workspace.dto;

import java.util.List;

public record ThreatIntelQueryResponse(
        String providerCode,
        String providerName,
        boolean configured,
        boolean executionSupported,
        String status,
        String query,
        List<ThreatIntelExposureDto> items,
        String error
) {
}
