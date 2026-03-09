package com.lume.workspace.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "email deve ser informado")
        @Email(message = "email deve ser um email valido")
        String email,
        @NotBlank(message = "password deve ser informado")
        String password
) {
}
