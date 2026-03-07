package com.lume.application.command;

/**
 * Comando para criação de um novo usuário.
 *
 * <p><b>Microservices Pattern: CQRS</b> - Objeto de comando imutável que encapsula
 * todos os dados necessários para a operação de escrita.</p>
 *
 * <p>Utiliza Java Record (Java 21) para imutabilidade e concisão.</p>
 */
public record CreateUserCommand(
        String name,
        String email,
        String password
) {
}
