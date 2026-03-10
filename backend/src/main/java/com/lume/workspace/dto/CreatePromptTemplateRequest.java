package com.lume.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreatePromptTemplateRequest(
        @NotBlank(message = "O titulo do template e obrigatorio")
        @Size(max = 200, message = "O titulo do template aceita ate 200 caracteres")
        String title,
        @NotBlank(message = "O resumo do template e obrigatorio")
        String summary,
        @NotBlank(message = "O corpo do prompt e obrigatorio")
        String promptBody,
        String templateScope,
        String projectId,
        String agentProfileId,
        List<String> variables,
        Boolean favorited,
        String statusLabel,
        String availability
) {
}
