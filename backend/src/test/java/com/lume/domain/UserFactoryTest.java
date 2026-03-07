package com.lume.domain;

import com.lume.domain.exception.BusinessRuleException;
import com.lume.domain.factory.UserFactory;
import com.lume.domain.model.User;
import com.lume.domain.service.PasswordEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para UserFactory (Factory Pattern).
 */
@DisplayName("UserFactory - Factory Pattern")
class UserFactoryTest {

    private PasswordEncoder passwordEncoder;
    private UserFactory userFactory;

    @BeforeEach
    void setUp() {
        passwordEncoder = mock(PasswordEncoder.class);
        userFactory = new UserFactory(passwordEncoder);
    }

    @Test
    @DisplayName("Deve criar usuário com senha codificada")
    void shouldCreateUserWithEncodedPassword() {
        when(passwordEncoder.encode("senha123")).thenReturn("$2a$12$encoded");

        User user = userFactory.createUser("João", "joao@email.com", "senha123");

        assertNotNull(user);
        assertEquals("João", user.getName());
        assertEquals("joao@email.com", user.getEmail());
        assertEquals("$2a$12$encoded", user.getPassword());
        assertTrue(user.isActive());
        verify(passwordEncoder).encode("senha123");
    }

    @Test
    @DisplayName("Deve rejeitar criação com nome inválido")
    void shouldRejectInvalidName() {
        assertThrows(BusinessRuleException.class, () ->
                userFactory.createUser("", "joao@email.com", "senha123")
        );
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("Deve rejeitar criação com e-mail inválido")
    void shouldRejectInvalidEmail() {
        assertThrows(BusinessRuleException.class, () ->
                userFactory.createUser("João", "invalido", "senha123")
        );
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("Deve rejeitar criação com senha inválida")
    void shouldRejectInvalidPassword() {
        assertThrows(BusinessRuleException.class, () ->
                userFactory.createUser("João", "joao@email.com", "123")
        );
        verifyNoInteractions(passwordEncoder);
    }
}
