package com.lume.application.handler.command;

import com.lume.application.command.CreateUserCommand;
import com.lume.application.command.UpdateUserCommand;
import com.lume.application.dto.request.UpdateUserRequestDTO;
import com.lume.application.dto.request.UserRequestDTO;
import com.lume.application.dto.response.UserResponseDTO;
import com.lume.application.mapper.UserMapper;
import com.lume.domain.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para UserMapper (ACL - Anti-Corruption Layer).
 */
@DisplayName("UserMapper - ACL")
class UserMapperTest {

    @Test
    @DisplayName("Deve converter User para UserResponseDTO")
    void shouldMapUserToResponseDTO() {
        User user = User.builder()
                .id(1L).name("João").email("joao@email.com").password("encoded").build();

        UserResponseDTO dto = UserMapper.toResponseDTO(user);

        assertEquals(1L, dto.id());
        assertEquals("João", dto.name());
        assertEquals("joao@email.com", dto.email());
        assertTrue(dto.active());
        assertNotNull(dto.createdAt());
    }

    @Test
    @DisplayName("Deve converter UserRequestDTO para CreateUserCommand")
    void shouldMapRequestToCreateCommand() {
        var dto = new UserRequestDTO("João", "joao@email.com", "senha123");

        CreateUserCommand command = UserMapper.toCreateCommand(dto);

        assertEquals("João", command.name());
        assertEquals("joao@email.com", command.email());
        assertEquals("senha123", command.password());
    }

    @Test
    @DisplayName("Deve converter UpdateUserRequestDTO para UpdateUserCommand com ID")
    void shouldMapRequestToUpdateCommand() {
        var dto = new UpdateUserRequestDTO("João", "joao@email.com", "senha123");

        UpdateUserCommand command = UserMapper.toUpdateCommand(5L, dto);

        assertEquals(5L, command.id());
        assertEquals("João", command.name());
        assertEquals("joao@email.com", command.email());
        assertEquals("senha123", command.password());
    }

    @Test
    @DisplayName("Deve converter UpdateUserRequestDTO sem senha para UpdateUserCommand")
    void shouldMapRequestToUpdateCommandWithoutPassword() {
        var dto = new UpdateUserRequestDTO("João", "joao@email.com", null);

        UpdateUserCommand command = UserMapper.toUpdateCommand(5L, dto);

        assertEquals(5L, command.id());
        assertEquals("João", command.name());
        assertEquals("joao@email.com", command.email());
        assertNull(command.password());
    }
}
