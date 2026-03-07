package com.lume.application.handler.command;

import com.lume.application.command.UpdateUserCommand;
import com.lume.application.dto.response.UserResponseDTO;
import com.lume.application.port.output.UserRepositoryPort;
import com.lume.domain.exception.BusinessRuleException;
import com.lume.domain.exception.ResourceNotFoundException;
import com.lume.domain.model.User;
import com.lume.domain.service.PasswordEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para UpdateUserCommandHandler (CQRS Command Handler).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateUserCommandHandler")
class UpdateUserCommandHandlerTest {

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UpdateUserCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new UpdateUserCommandHandler(userRepository, passwordEncoder);
    }

    private User createExistingUser() {
        return User.builder()
                .id(1L)
                .name("João Silva")
                .email("joao@email.com")
                .password("$2a$12$oldEncoded")
                .build();
    }

    @Test
    @DisplayName("Deve atualizar usuário com sucesso")
    void shouldUpdateUserSuccessfully() {
        var command = new UpdateUserCommand(1L, "João Atualizado", "joao@email.com", "novaSenha");
        User existingUser = createExistingUser();

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.encode("novaSenha")).thenReturn("$2a$12$newEncoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponseDTO result = handler.handle(command);

        assertNotNull(result);
        assertEquals("João Atualizado", result.name());
        verify(passwordEncoder).encode("novaSenha");
    }

    @Test
    @DisplayName("Deve atualizar sem alterar senha quando senha é vazia")
    void shouldUpdateWithoutChangingPasswordWhenEmpty() {
        var command = new UpdateUserCommand(1L, "João Atualizado", "joao@email.com", "");
        User existingUser = createExistingUser();

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponseDTO result = handler.handle(command);

        assertNotNull(result);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("Deve lançar exceção quando usuário não é encontrado")
    void shouldThrowWhenUserNotFound() {
        var command = new UpdateUserCommand(99L, "Test", "test@email.com", "senha123");
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> handler.handle(command));
    }

    @Test
    @DisplayName("Deve lançar exceção quando e-mail pertence a outro usuário")
    void shouldThrowWhenEmailBelongsToAnotherUser() {
        var command = new UpdateUserCommand(1L, "Test", "outro@email.com", "senha123");
        User existingUser = createExistingUser();
        User otherUser = User.builder().id(2L).name("Outro").email("outro@email.com").password("p").build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.findByEmail("outro@email.com")).thenReturn(Optional.of(otherUser));

        assertThrows(BusinessRuleException.class, () -> handler.handle(command));
    }
}
