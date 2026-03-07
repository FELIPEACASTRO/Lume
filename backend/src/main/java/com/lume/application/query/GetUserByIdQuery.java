package com.lume.application.query;

/**
 * Query para busca de usuário por ID.
 *
 * <p><b>Microservices Pattern: CQRS</b> - Objeto de query imutável.</p>
 */
public record GetUserByIdQuery(Long id) {
}
