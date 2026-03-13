package com.lume.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
        @NotBlank(message = "name deve ser informado")
        @Size(max = 160, message = "name deve ter no maximo 160 caracteres")
        String name,
        @NotBlank(message = "summary deve ser informado")
        String summary,
        @Size(max = 80, message = "statusLabel deve ter no maximo 80 caracteres")
        String statusLabel,
        @Size(max = 32, message = "availability deve ter no maximo 32 caracteres")
        String availability,
        @Size(max = 120, message = "ownerName deve ter no maximo 120 caracteres")
        String ownerName
) {
}
