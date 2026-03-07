package com.lume.application.handler.command;

import com.lume.application.port.output.UserRepositoryPort;
import com.lume.domain.exception.ResourceNotFoundException;
import com.lume.domain.model.User;
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
 * Testes unitários para DeleteUserCommandHandler (CQRS Command Handler).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteUserCommandHandler")
class DeleteUserCommandHandlerTest {

    @Mock
    private UserRepositoryPort userRepository;

    private DeleteUserCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new DeleteUserCommandHandler(userRepository);
    }

    @Test
    @DisplayName("Deve desativar usuário com sucesso (soft delete)")
    void shouldDeactivateUserSuccessfully() {
        User user = User.builder()
                .id(1L).name("João").email("joao@email.com").password("p").build();
        assertTrue(user.isActive());

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        handler.handle(1L);

        assertFalse(user.isActive());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Deve lançar exceção quando usuário não é encontrado")
    void shouldThrowWhenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> handler.handle(99L));
        verify(userRepository, never()).save(any());
    }
}
