package com.lume.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para operações com usuários.
 *
 * <p><b>Microservices Pattern: ACL (Anti-Corruption Layer)</b> - Este DTO atua como
 * camada anticorrupção, isolando o modelo de domínio da representação externa (API).
 * Mudanças na API não afetam o domínio e vice-versa.</p>
 *
 * <p>Utiliza Bean Validation para validação na camada de apresentação.</p>
 */
public record UserRequestDTO(

        @NotBlank(message = "O nome é obrigatório")
        @Size(min = 2, max = 150, message = "O nome deve ter entre 2 e 150 caracteres")
        String name,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Formato de e-mail inválido")
        String email,

        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 6, max = 100, message = "A senha deve ter entre 6 e 100 caracteres")
        String password
) {
}
