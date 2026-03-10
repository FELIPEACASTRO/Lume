package com.lume.workspace.dto;

import jakarta.validation.constraints.Size;

public record UpdateWorkspaceOnboardingRequest(
        @Size(max = 64, message = "primaryUseCase nao pode ultrapassar 64 caracteres")
        String primaryUseCase,
        @Size(max = 64, message = "workStyle nao pode ultrapassar 64 caracteres")
        String workStyle,
        @Size(max = 32, message = "activationStatus nao pode ultrapassar 32 caracteres")
        String activationStatus,
        @Size(max = 255, message = "activationNote nao pode ultrapassar 255 caracteres")
        String activationNote
) {
}
