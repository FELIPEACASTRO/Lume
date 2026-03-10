package com.lume.workspace.dto;

import java.util.List;

public record TaskDetailResponse(
        TaskSummaryResponse task,
        List<TaskStepResponse> steps,
        List<String> followUpSuggestions
) {
}
