package com.lume.workspace.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateAgentMessageRequest(
        @NotBlank(message = "A mensagem e obrigatoria")
        String message
) {
}
