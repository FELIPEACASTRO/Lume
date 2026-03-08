package com.lume.workspace.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateMemberRequest(
        @NotBlank(message = "Nome e obrigatorio")
        @Size(min = 2, max = 150, message = "Nome deve ter entre 2 e 150 caracteres")
        String name,
        @NotBlank(message = "Email e obrigatorio")
        @Email(message = "Email invalido")
        String email,
        @NotBlank(message = "Senha e obrigatoria")
        @Size(min = 6, max = 100, message = "Senha deve ter entre 6 e 100 caracteres")
        String password,
        @NotBlank(message = "Role e obrigatoria")
        String roleCode
) {
}
