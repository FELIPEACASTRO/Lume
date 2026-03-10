package com.lume.workspace.dto;

public record TaskStepResponse(
        String id,
        int stepOrder,
        String stepType,
        String title,
        String detail,
        String statusLabel
) {
}
