package com.lume.application.dto.response;

import java.time.LocalDateTime;

/**
 * DTO de saída para representação de usuários na API.
 *
 * <p><b>Microservices Pattern: ACL (Anti-Corruption Layer)</b> - Isola a representação
 * externa do modelo de domínio. Campos sensíveis (como senha) são excluídos.</p>
 *
 * <p>Utiliza Java Record (Java 21) para imutabilidade e concisão.</p>
 */
public record UserResponseDTO(
        Long id,
        String name,
        String email,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
