package com.lume.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lume.application.dto.request.UserRequestDTO;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração para os controllers de usuário.
 *
 * <p>Utiliza o perfil "test" com banco H2 em memória para isolamento.</p>
 * <p>Testa o fluxo completo: Controller → Service → Repository → Banco.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("User Controllers - Testes de Integração")
class UserControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @Order(1)
    @DisplayName("POST /api/users - Deve criar um usuário com sucesso")
    void shouldCreateUser() throws Exception {
        var request = new UserRequestDTO("João Silva", "joao@email.com", "senha123");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("João Silva"))
                .andExpect(jsonPath("$.email").value("joao@email.com"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/users - Deve rejeitar e-mail duplicado")
    void shouldRejectDuplicateEmail() throws Exception {
        var request = new UserRequestDTO("Outro Nome", "joao@email.com", "senha456");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @Order(3)
    @DisplayName("POST /api/users - Deve rejeitar dados inválidos")
    void shouldRejectInvalidData() throws Exception {
        var request = new UserRequestDTO("", "invalido", "12");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").exists());
    }

    @Test
    @Order(4)
    @DisplayName("GET /api/users - Deve listar usuários paginados")
    void shouldListUsers() throws Exception {
        mockMvc.perform(get("/api/users")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    @Order(5)
    @DisplayName("GET /api/users/{id} - Deve buscar usuário por ID")
    void shouldFindUserById() throws Exception {
        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("João Silva"));
    }

    @Test
    @Order(6)
    @DisplayName("GET /api/users/{id} - Deve retornar 404 para ID inexistente")
    void shouldReturn404ForNonExistentId() throws Exception {
        mockMvc.perform(get("/api/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @Order(7)
    @DisplayName("PUT /api/users/{id} - Deve atualizar usuário com sucesso")
    void shouldUpdateUser() throws Exception {
        var request = new UserRequestDTO("João Atualizado", "joao@email.com", "novaSenha");

        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("João Atualizado"));
    }

    @Test
    @Order(8)
    @DisplayName("DELETE /api/users/{id} - Deve desativar usuário com sucesso")
    void shouldDeleteUser() throws Exception {
        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isNoContent());

        // Verificar que o usuário foi desativado
        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @Order(9)
    @DisplayName("GET /api/health - Deve retornar status UP")
    void shouldReturnHealthStatus() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.application").value("Lume Backend"));
    }
}
