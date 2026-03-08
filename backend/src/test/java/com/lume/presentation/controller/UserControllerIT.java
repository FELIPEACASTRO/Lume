package com.lume.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lume.application.dto.request.UpdateUserRequestDTO;
import com.lume.application.dto.request.UserRequestDTO;
import com.lume.infrastructure.persistence.repository.JpaUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("User Controllers - Integration Tests")
class UserControllerIT {

    private static final String CREATED_USER_NAME = "Joao Silva";
    private static final String CREATED_USER_EMAIL = "joao@email.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JpaUserRepository userRepository;

    @Test
    @Order(1)
    @DisplayName("POST /api/users - should create a user")
    void shouldCreateUser() throws Exception {
        UserRequestDTO request = new UserRequestDTO(CREATED_USER_NAME, CREATED_USER_EMAIL, "senha123");

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value(CREATED_USER_NAME))
                .andExpect(jsonPath("$.email").value(CREATED_USER_EMAIL))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/users - should reject duplicate email")
    void shouldRejectDuplicateEmail() throws Exception {
        UserRequestDTO request = new UserRequestDTO("Outro Nome", CREATED_USER_EMAIL, "senha456");

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @Order(3)
    @DisplayName("POST /api/users - should reject invalid data")
    void shouldRejectInvalidData() throws Exception {
        UserRequestDTO request = new UserRequestDTO("", "invalido", "12");

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").exists());
    }

    @Test
    @Order(4)
    @DisplayName("GET /api/users - should list paginated users")
    void shouldListUsers() throws Exception {
        mockMvc.perform(get("/users")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    @Order(5)
    @DisplayName("GET /api/users/{id} - should find created user by id")
    void shouldFindUserById() throws Exception {
        Long createdUserId = createdUserId();

        mockMvc.perform(get("/users/{id}", createdUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdUserId))
                .andExpect(jsonPath("$.name").value(CREATED_USER_NAME));
    }

    @Test
    @Order(6)
    @DisplayName("GET /api/users/{id} - should return 404 for missing id")
    void shouldReturn404ForNonExistentId() throws Exception {
        mockMvc.perform(get("/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @Order(7)
    @DisplayName("PUT /api/users/{id} - should update the created user")
    void shouldUpdateUser() throws Exception {
        Long createdUserId = createdUserId();
        UpdateUserRequestDTO request = new UpdateUserRequestDTO("Joao Atualizado", CREATED_USER_EMAIL, "novaSenha");

        mockMvc.perform(put("/users/{id}", createdUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Joao Atualizado"));
    }

    @Test
    @Order(8)
    @DisplayName("PUT /api/users/{id} - should update the user without changing password")
    void shouldUpdateUserWithoutPassword() throws Exception {
        Long createdUserId = createdUserId();
        UpdateUserRequestDTO request = new UpdateUserRequestDTO("Joao Sem Nova Senha", CREATED_USER_EMAIL, null);

        mockMvc.perform(put("/users/{id}", createdUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Joao Sem Nova Senha"));
    }

    @Test
    @Order(9)
    @DisplayName("DELETE /api/users/{id} - should deactivate the created user")
    void shouldDeleteUser() throws Exception {
        Long createdUserId = createdUserId();

        mockMvc.perform(delete("/users/{id}", createdUserId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/users/{id}", createdUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @Order(10)
    @DisplayName("GET /api/health - should return UP")
    void shouldReturnHealthStatus() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.application").value("Lume Backend"));
    }

    private Long createdUserId() {
        return userRepository.findByEmail(CREATED_USER_EMAIL)
                .orElseThrow(() -> new IllegalStateException("Created test user was not found"))
                .getId();
    }
}
