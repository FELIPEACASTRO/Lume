package com.lume.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para atualizacao de usuarios.
 *
 * <p>A senha e opcional durante a edicao. Quando omitida ou enviada em branco,
 * a senha atual do usuario e preservada.</p>
 */
public record UpdateUserRequestDTO(

        @NotBlank(message = "O nome e obrigatorio")
        @Size(min = 2, max = 150, message = "O nome deve ter entre 2 e 150 caracteres")
        String name,

        @NotBlank(message = "O e-mail e obrigatorio")
        @Email(message = "Formato de e-mail invalido")
        String email,

        @Size(min = 6, max = 100, message = "A senha deve ter entre 6 e 100 caracteres")
        String password
) {
}
