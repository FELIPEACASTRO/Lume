package com.lume.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSupportTicketRequest(
        @NotBlank(message = "O titulo do ticket e obrigatorio")
        @Size(max = 200, message = "O titulo aceita ate 200 caracteres")
        String title,
        @NotBlank(message = "A descricao do ticket e obrigatoria")
        String description,
        String category,
        String severity
) {
}
