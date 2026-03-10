package com.lume.workspace.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateAgentThreadRequest(
        @NotBlank(message = "O agentProfileId e obrigatorio")
        String agentProfileId,
        @NotBlank(message = "A mensagem inicial e obrigatoria")
        String message
) {
}
