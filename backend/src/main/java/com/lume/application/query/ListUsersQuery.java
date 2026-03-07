package com.lume.application.query;

import org.springframework.data.domain.Pageable;

/**
 * Query para listagem paginada de usuários.
 *
 * <p><b>Microservices Pattern: CQRS</b> - Objeto de query imutável com parâmetros de paginação.</p>
 */
public record ListUsersQuery(Pageable pageable) {
}
