package com.lume.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.lume.workspace.dto.CreateProjectRequest;
import com.lume.workspace.dto.UpdateProjectRequest;
import com.lume.workspace.service.WorkspaceContextService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Projects write endpoints - Integration Tests")
class ProjectWriteIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/v1/projects - should create project with valid payload")
    void shouldCreateProject() throws Exception {
        CreateProjectRequest request = new CreateProjectRequest(
                "Projeto Integracao",
                "Projeto criado pela suite de integracao.",
                null,
                null,
                null
        );

        mockMvc.perform(post("/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.name").value("Projeto Integracao"))
                .andExpect(jsonPath("$.summary").value("Projeto criado pela suite de integracao."))
                .andExpect(jsonPath("$.statusLabel").value("Em andamento"))
                .andExpect(jsonPath("$.availability").value("live"))
                .andExpect(jsonPath("$.ownerName").isString());
    }

    @Test
    @DisplayName("POST /api/v1/projects - should reject invalid payload")
    void shouldRejectInvalidProjectCreatePayload() throws Exception {
        CreateProjectRequest request = new CreateProjectRequest(
                "",
                "",
                null,
                null,
                null
        );

        mockMvc.perform(post("/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Erro de validacao"))
                .andExpect(jsonPath("$.details.name").exists())
                .andExpect(jsonPath("$.details.summary").exists());
    }

    @Test
    @DisplayName("PATCH /api/v1/projects/{id} - should update project when it exists")
    void shouldUpdateProject() throws Exception {
        CreateProjectRequest createRequest = new CreateProjectRequest(
                "Projeto Atualizar",
                "Resumo inicial.",
                "Em andamento",
                "live",
                "Operador"
        );

        String createdBody = mockMvc.perform(post("/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String projectId = JsonPath.read(createdBody, "$.id");

        UpdateProjectRequest updateRequest = new UpdateProjectRequest(
                "Projeto Atualizado",
                "Resumo atualizado pela IT.",
                "Concluido",
                "restricted",
                "Operador QA"
        );

        mockMvc.perform(patch("/v1/projects/{id}", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(projectId))
                .andExpect(jsonPath("$.name").value("Projeto Atualizado"))
                .andExpect(jsonPath("$.summary").value("Resumo atualizado pela IT."))
                .andExpect(jsonPath("$.statusLabel").value("Concluido"))
                .andExpect(jsonPath("$.availability").value("restricted"))
                .andExpect(jsonPath("$.ownerName").value("Operador QA"));
    }

    @Test
    @DisplayName("PATCH /api/v1/projects/{id} - should return 404 for unknown project")
    void shouldReturnNotFoundForUnknownProject() throws Exception {
        UpdateProjectRequest updateRequest = new UpdateProjectRequest(
                "Projeto inexistente",
                "Sem efeito",
                null,
                null,
                null
        );

        mockMvc.perform(patch("/v1/projects/{id}", "proj-nao-existe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Projeto nao encontrado(a) com id: proj-nao-existe"));
    }

    @Test
    @DisplayName("POST /api/v1/projects - should return forbidden when actor header is invalid")
    void shouldReturnForbiddenWhenActorHeaderIsInvalid() throws Exception {
        CreateProjectRequest request = new CreateProjectRequest(
                "Projeto proibido",
                "Sem permissao para executar",
                null,
                null,
                null
        );

        mockMvc.perform(post("/v1/projects")
                        .header(WorkspaceContextService.HEADER_ACTOR_USER_ID, "ator-invalido")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("O header de usuario atual e invalido."));
    }
}

