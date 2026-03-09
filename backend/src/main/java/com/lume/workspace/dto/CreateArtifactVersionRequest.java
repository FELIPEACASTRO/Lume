package com.lume.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateArtifactVersionRequest(
        @NotBlank(message = "A versao do artefato e obrigatoria")
        @Size(max = 80, message = "A versao do artefato aceita ate 80 caracteres")
        String versionLabel,
        @NotBlank(message = "O resumo da mudanca e obrigatorio")
        @Size(max = 240, message = "O resumo da mudanca aceita ate 240 caracteres")
        String changeSummary,
        @NotBlank(message = "O preview do conteudo e obrigatorio")
        String contentPreview
) {
}
