package com.lume.workspace.service;

import com.lume.workspace.dto.FinopsAnomalyResponse;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@FunctionalInterface
public interface FinopsAnomalyDetector {

    List<FinopsAnomalyResponse> detect(Long workspaceId, LocalDateTime now);

    static FinopsAnomalyResponse anomaly(
            String code,
            String severity,
            String title,
            String detail,
            String recommendedAction,
            LocalDateTime detectedAt
    ) {
        return new FinopsAnomalyResponse(
                code,
                severity,
                "open",
                title,
                detail,
                recommendedAction,
                DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(detectedAt)
        );
    }
}
