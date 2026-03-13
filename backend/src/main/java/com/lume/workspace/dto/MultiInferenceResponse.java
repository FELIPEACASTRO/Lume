package com.lume.workspace.dto;

import java.util.List;

public record MultiInferenceResponse(
        MultiInferenceRunResponse winner,
        List<MultiInferenceRunResponse> runs,
        List<String> rankingReasons,
        MultiInferenceCostSummaryResponse costSummary,
        String policyDecisionSummary,
        String rankingPolicy,
        String routingPolicy
) {
}
