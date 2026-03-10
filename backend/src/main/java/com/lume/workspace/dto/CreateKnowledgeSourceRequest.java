package com.lume.workspace.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateKnowledgeSourceRequest(
        @NotBlank(message = "title deve ser informado")
        @Size(max = 180, message = "title deve ter no maximo 180 caracteres")
        String title,
        @NotBlank(message = "sourceType deve ser informado")
        @Size(max = 80, message = "sourceType deve ter no maximo 80 caracteres")
        String sourceType,
        @Size(max = 64, message = "projectId deve ter no maximo 64 caracteres")
        String projectId,
        @Size(max = 512, message = "sourceUri deve ter no maximo 512 caracteres")
        String sourceUri,
        @Min(value = 0, message = "documentCount nao pode ser negativo")
        Integer documentCount,
        Boolean enabledForAgents,
        @Size(max = 80, message = "statusLabel deve ter no maximo 80 caracteres")
        String statusLabel,
        @Size(max = 32, message = "availability deve ter no maximo 32 caracteres")
        String availability,
        @NotBlank(message = "note deve ser informado")
        String note
) {
}
