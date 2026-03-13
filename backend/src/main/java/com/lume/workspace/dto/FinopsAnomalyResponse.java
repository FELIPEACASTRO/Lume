package com.lume.workspace.dto;

public record FinopsAnomalyResponse(
        String code,
        String severity,
        String status,
        String title,
        String detail,
        String recommendedAction,
        String detectedAt
) {
}
