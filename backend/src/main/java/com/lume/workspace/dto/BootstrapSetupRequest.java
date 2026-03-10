package com.lume.workspace.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BootstrapSetupRequest(
        @NotBlank(message = "organizationName deve ser informado")
        @Size(max = 160, message = "organizationName nao pode ultrapassar 160 caracteres")
        String organizationName,
        @NotBlank(message = "workspaceName deve ser informado")
        @Size(max = 160, message = "workspaceName nao pode ultrapassar 160 caracteres")
        String workspaceName,
        @NotBlank(message = "adminName deve ser informado")
        @Size(max = 150, message = "adminName nao pode ultrapassar 150 caracteres")
        String adminName,
        @NotBlank(message = "adminEmail deve ser informado")
        @Email(message = "adminEmail deve ser um email valido")
        String adminEmail,
        @NotBlank(message = "password deve ser informado")
        @Size(min = 8, max = 128, message = "password deve ter entre 8 e 128 caracteres")
        String password
) {
}
