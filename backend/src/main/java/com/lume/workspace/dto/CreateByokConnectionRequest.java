package com.lume.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateByokConnectionRequest(
        @NotBlank(message = "providerCode e obrigatorio")
        String providerCode,
        @NotBlank(message = "connectionName e obrigatorio")
        @Size(max = 160, message = "connectionName aceita ate 160 caracteres")
        String connectionName,
        @NotBlank(message = "secretRef e obrigatorio")
        @Size(max = 160, message = "secretRef aceita ate 160 caracteres")
        String secretRef,
        String scopeLabel
) {
}
