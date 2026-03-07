package com.lume.infrastructure.persistence;

import com.lume.domain.model.User;
import com.lume.infrastructure.persistence.entity.UserJpaEntity;
import com.lume.infrastructure.persistence.mapper.UserPersistenceMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para UserPersistenceMapper (ACL entre domínio e JPA).
 */
@DisplayName("UserPersistenceMapper - ACL")
class UserPersistenceMapperTest {

    @Test
    @DisplayName("Deve converter JpaEntity para domínio User")
    void shouldMapJpaEntityToDomain() {
        UserJpaEntity entity = new UserJpaEntity();
        entity.setId(1L);
        entity.setName("João");
        entity.setEmail("joao@email.com");
        entity.setPassword("encoded");
        entity.setActive(true);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        User user = UserPersistenceMapper.toDomain(entity);

        assertEquals(1L, user.getId());
        assertEquals("João", user.getName());
        assertEquals("joao@email.com", user.getEmail());
        assertEquals("encoded", user.getPassword());
        assertTrue(user.isActive());
    }

    @Test
    @DisplayName("Deve converter domínio User para JpaEntity")
    void shouldMapDomainToJpaEntity() {
        User user = User.builder()
                .id(1L)
                .name("João")
                .email("joao@email.com")
                .password("encoded")
                .active(true)
                .build();

        UserJpaEntity entity = UserPersistenceMapper.toJpaEntity(user);

        assertEquals(1L, entity.getId());
        assertEquals("João", entity.getName());
        assertEquals("joao@email.com", entity.getEmail());
        assertEquals("encoded", entity.getPassword());
        assertTrue(entity.isActive());
    }
}
