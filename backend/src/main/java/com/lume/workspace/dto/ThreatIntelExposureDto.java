package com.lume.workspace.dto;

public record ThreatIntelExposureDto(
        String title,
        String source,
        String severity,
        String excerpt,
        String reference
) {
}
