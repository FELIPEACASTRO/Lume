package com.lume.infrastructure.persistence.mapper;

import com.lume.domain.model.User;
import com.lume.infrastructure.persistence.entity.UserJpaEntity;

/**
 * Mapper entre a entidade de domínio User e a entidade JPA UserJpaEntity.
 *
 * <p><b>Microservices Pattern: ACL (Anti-Corruption Layer)</b> - Traduz entre
 * o modelo de domínio e o modelo de persistência, garantindo que mudanças
 * no esquema do banco não afetem o domínio.</p>
 *
 * <p><b>Complexidade: O(1)</b> para ambas as direções de mapeamento.</p>
 */
public final class UserPersistenceMapper {

    private UserPersistenceMapper() {
        // Classe utilitária
    }

    /**
     * Converte uma entidade JPA para entidade de domínio.
     */
    public static User toDomain(UserJpaEntity entity) {
        return User.builder()
                .id(entity.getId())
                .name(entity.getName())
                .email(entity.getEmail())
                .password(entity.getPassword())
                .active(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Converte uma entidade de domínio para entidade JPA.
     */
    public static UserJpaEntity toJpaEntity(User user) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.setId(user.getId());
        entity.setName(user.getName());
        entity.setEmail(user.getEmail());
        entity.setPassword(user.getPassword());
        entity.setActive(user.isActive());
        entity.setCreatedAt(user.getCreatedAt());
        entity.setUpdatedAt(user.getUpdatedAt());
        return entity;
    }
}
