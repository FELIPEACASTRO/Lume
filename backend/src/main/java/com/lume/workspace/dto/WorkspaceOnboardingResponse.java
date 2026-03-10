package com.lume.workspace.dto;

public record WorkspaceOnboardingResponse(
        String primaryUseCase,
        String workStyle,
        String activationStatus,
        String activationNote
) {
}
