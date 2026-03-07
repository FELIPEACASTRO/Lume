package com.lume.application.handler.command;

import com.lume.application.command.CreateUserCommand;
import com.lume.application.dto.response.UserResponseDTO;
import com.lume.application.port.output.UserRepositoryPort;
import com.lume.domain.exception.BusinessRuleException;
import com.lume.domain.factory.UserFactory;
import com.lume.domain.model.User;
import com.lume.domain.service.PasswordEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para CreateUserCommandHandler (CQRS Command Handler).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreateUserCommandHandler")
class CreateUserCommandHandlerTest {

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private CreateUserCommandHandler handler;

    @BeforeEach
    void setUp() {
        UserFactory userFactory = new UserFactory(passwordEncoder);
        handler = new CreateUserCommandHandler(userRepository, userFactory);
    }

    @Test
    @DisplayName("Deve criar usuário com sucesso quando e-mail é único")
    void shouldCreateUserWhenEmailIsUnique() {
        var command = new CreateUserCommand("João Silva", "joao@email.com", "senha123");

        when(userRepository.existsByEmail("joao@email.com")).thenReturn(false);
        when(passwordEncoder.encode("senha123")).thenReturn("$2a$12$encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return User.builder()
                    .id(1L)
                    .name(user.getName())
                    .email(user.getEmail())
                    .password(user.getPassword())
                    .active(user.isActive())
                    .createdAt(user.getCreatedAt())
                    .updatedAt(user.getUpdatedAt())
                    .build();
        });

        UserResponseDTO result = handler.handle(command);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("João Silva", result.name());
        assertEquals("joao@email.com", result.email());
        assertTrue(result.active());

        verify(userRepository).existsByEmail("joao@email.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Deve lançar exceção quando e-mail já está cadastrado")
    void shouldThrowWhenEmailAlreadyExists() {
        var command = new CreateUserCommand("João Silva", "joao@email.com", "senha123");
        when(userRepository.existsByEmail("joao@email.com")).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> handler.handle(command));

        verify(userRepository).existsByEmail("joao@email.com");
        verify(userRepository, never()).save(any());
    }
}
