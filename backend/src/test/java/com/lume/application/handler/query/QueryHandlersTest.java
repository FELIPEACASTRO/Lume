package com.lume.application.handler.query;

import com.lume.application.dto.response.UserResponseDTO;
import com.lume.application.port.output.UserRepositoryPort;
import com.lume.application.query.GetUserByIdQuery;
import com.lume.application.query.ListUsersQuery;
import com.lume.domain.exception.ResourceNotFoundException;
import com.lume.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para os Query Handlers (CQRS).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Query Handlers (CQRS)")
class QueryHandlersTest {

    @Mock
    private UserRepositoryPort userRepository;

    private GetUserByIdQueryHandler getByIdHandler;
    private ListUsersQueryHandler listHandler;

    @BeforeEach
    void setUp() {
        getByIdHandler = new GetUserByIdQueryHandler(userRepository);
        listHandler = new ListUsersQueryHandler(userRepository);
    }

    private User createUser(Long id, String name, String email) {
        return User.builder()
                .id(id).name(name).email(email).password("encoded").build();
    }

    @Nested
    @DisplayName("GetUserByIdQueryHandler")
    class GetByIdTests {

        @Test
        @DisplayName("Deve retornar usuário quando encontrado")
        void shouldReturnUserWhenFound() {
            User user = createUser(1L, "João", "joao@email.com");
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            UserResponseDTO result = getByIdHandler.handle(new GetUserByIdQuery(1L));

            assertNotNull(result);
            assertEquals(1L, result.id());
            assertEquals("João", result.name());
            assertEquals("joao@email.com", result.email());
        }

        @Test
        @DisplayName("Deve lançar exceção quando não encontrado")
        void shouldThrowWhenNotFound() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () ->
                    getByIdHandler.handle(new GetUserByIdQuery(99L))
            );
        }
    }

    @Nested
    @DisplayName("ListUsersQueryHandler")
    class ListTests {

        @Test
        @DisplayName("Deve retornar página de usuários")
        void shouldReturnPageOfUsers() {
            Pageable pageable = PageRequest.of(0, 20);
            List<User> users = List.of(
                    createUser(1L, "João", "joao@email.com"),
                    createUser(2L, "Maria", "maria@email.com")
            );
            Page<User> page = new PageImpl<>(users, pageable, 2);

            when(userRepository.findAll(pageable)).thenReturn(page);

            Page<UserResponseDTO> result = listHandler.handle(new ListUsersQuery(pageable));

            assertNotNull(result);
            assertEquals(2, result.getTotalElements());
            assertEquals("João", result.getContent().get(0).name());
            assertEquals("Maria", result.getContent().get(1).name());
        }

        @Test
        @DisplayName("Deve retornar página vazia quando não há usuários")
        void shouldReturnEmptyPage() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<User> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(userRepository.findAll(pageable)).thenReturn(emptyPage);

            Page<UserResponseDTO> result = listHandler.handle(new ListUsersQuery(pageable));

            assertNotNull(result);
            assertEquals(0, result.getTotalElements());
            assertTrue(result.getContent().isEmpty());
        }
    }
}
