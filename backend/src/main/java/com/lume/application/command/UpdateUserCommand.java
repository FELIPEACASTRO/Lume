package com.lume.application.command;

/**
 * Comando para atualização de um usuário existente.
 *
 * <p><b>Microservices Pattern: CQRS</b> - Objeto de comando imutável.</p>
 */
public record UpdateUserCommand(
        Long id,
        String name,
        String email,
        String password
) {
}
