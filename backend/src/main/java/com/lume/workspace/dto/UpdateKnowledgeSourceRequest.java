package com.lume.workspace.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateKnowledgeSourceRequest(
        @Size(max = 180, message = "title deve ter no maximo 180 caracteres")
        String title,
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
        String note
) {
}
