package com.lume.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lume.infrastructure.persistence.entity.UserJpaEntity;
import com.lume.infrastructure.persistence.repository.JpaUserRepository;
import com.lume.workspace.controller.VersionedShellCatalogController;
import com.lume.workspace.dto.CreateShellNavigationItemRequest;
import com.lume.workspace.dto.CreateShellTaskTypeRequest;
import com.lume.workspace.dto.UpdateShellNavigationItemRequest;
import com.lume.workspace.dto.UpdateShellTaskTypeRequest;
import com.lume.workspace.entity.ShellNavigationItemJpaEntity;
import com.lume.workspace.entity.ShellTaskTypeJpaEntity;
import com.lume.workspace.repository.ShellNavigationItemJpaRepository;
import com.lume.workspace.repository.ShellTaskTypeJpaRepository;
import com.lume.workspace.service.WorkspaceContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(VersionedShellCatalogController.class)
@DisplayName("Shell Catalog Admin - Integration Tests")
class ShellCatalogAdminIT {

    private static final String OPERATOR_EMAIL = "operator@lume.local";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JpaUserRepository userRepository;

    @Autowired
    private ShellNavigationItemJpaRepository navigationItemRepository;

    @Autowired
    private ShellTaskTypeJpaRepository taskTypeRepository;

    @BeforeEach
    void resetCatalog() {
        ShellNavigationItemJpaEntity home = navigationItemRepository.findById("home").orElseGet(() -> {
            ShellNavigationItemJpaEntity entity = new ShellNavigationItemJpaEntity();
            entity.setId("home");
            entity.setPath("/");
            entity.setIcon("home");
            return entity;
        });
        home.setLabel("Inicio");
        home.setDescription("Resumo do trabalho e atalhos do workspace.");
        home.setAvailability("live");
        home.setNavGroup("primary");
        home.setSortOrder(10);
        home.setEnabled(true);
        home.setKeywordsRaw("inicio,resumo,atividade,atalhos");
        navigationItemRepository.save(home);

        ShellTaskTypeJpaEntity research = taskTypeRepository.findById("research").orElseGet(() -> {
            ShellTaskTypeJpaEntity entity = new ShellTaskTypeJpaEntity();
            entity.setTaskType("research");
            return entity;
        });
        research.setLabel("Pesquisar");
        research.setDescription("Levantar contexto, fontes e decisoes.");
        research.setSortOrder(10);
        research.setEnabled(true);
        taskTypeRepository.save(research);
    }

    @Test
    @DisplayName("GET /api/v1/shell/catalog - should return persisted catalog for settings admins")
    void shouldReturnShellCatalog() throws Exception {
        mockMvc.perform(get("/v1/shell/catalog").with(operatorHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value("home"))
                .andExpect(jsonPath("$.items[0].enabled").value(true))
                .andExpect(jsonPath("$.taskTypes[0].taskType").value("research"));
    }

    @Test
    @DisplayName("POST /api/v1/shell/catalog/navigation-items - should create a new area")
    void shouldCreateNavigationItem() throws Exception {
        CreateShellNavigationItemRequest request = new CreateShellNavigationItemRequest(
                "ops-alerts",
                "Alertas",
                "/ops-alerts",
                "Alertas e pendencias do workspace.",
                "inbox",
                "attention",
                "secondary",
                70,
                true,
                List.of("alertas", "pendencias")
        );

        mockMvc.perform(post("/v1/shell/catalog/navigation-items")
                        .with(operatorHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("ops-alerts"))
                .andExpect(jsonPath("$.path").value("/ops-alerts"))
                .andExpect(jsonPath("$.icon").value("inbox"));

        mockMvc.perform(get("/v1/shell/catalog").with(operatorHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id=='ops-alerts')]").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/v1/shell/catalog/navigation-items - should reject duplicate path")
    void shouldRejectDuplicateNavigationPath() throws Exception {
        CreateShellNavigationItemRequest request = new CreateShellNavigationItemRequest(
                "overview",
                "Visao geral",
                "/",
                "Resumo alternativo.",
                "home",
                "live",
                "primary",
                15,
                true,
                List.of("visao")
        );

        mockMvc.perform(post("/v1/shell/catalog/navigation-items")
                        .with(operatorHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("path ja esta em uso por outra area da shell."));
    }

    @Test
    @DisplayName("PATCH /api/v1/shell/catalog/navigation-items/{id} - should persist menu changes")
    void shouldUpdateNavigationItem() throws Exception {
        UpdateShellNavigationItemRequest request = new UpdateShellNavigationItemRequest(
                "Painel",
                "/inicio",
                "Centro do trabalho do workspace.",
                "search",
                "attention",
                "primary",
                5,
                true,
                List.of("painel", "trabalho", "inicio")
        );

        mockMvc.perform(patch("/v1/shell/catalog/navigation-items/home")
                        .with(operatorHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.label").value("Painel"))
                .andExpect(jsonPath("$.path").value("/inicio"))
                .andExpect(jsonPath("$.icon").value("search"))
                .andExpect(jsonPath("$.sortOrder").value(5))
                .andExpect(jsonPath("$.availability").value("attention"));

        mockMvc.perform(get("/v1/shell/navigation").with(operatorHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].label").value("Painel"))
                .andExpect(jsonPath("$.items[0].path").value("/inicio"))
                .andExpect(jsonPath("$.items[0].icon").value("search"));
    }

    @Test
    @DisplayName("PATCH /api/v1/shell/catalog/navigation-items/{id} - should reject invalid route")
    void shouldRejectInvalidNavigationRoute() throws Exception {
        UpdateShellNavigationItemRequest request = new UpdateShellNavigationItemRequest(
                null,
                "inicio sem barra",
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        mockMvc.perform(patch("/v1/shell/catalog/navigation-items/home")
                        .with(operatorHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("path deve comecar com '/'."));
    }

    @Test
    @DisplayName("PATCH /api/v1/shell/catalog/navigation-items/{id} - should reject invalid icon")
    void shouldRejectInvalidNavigationIcon() throws Exception {
        UpdateShellNavigationItemRequest request = new UpdateShellNavigationItemRequest(
                null,
                null,
                null,
                "rocket",
                null,
                null,
                null,
                null,
                null
        );

        mockMvc.perform(patch("/v1/shell/catalog/navigation-items/home")
                        .with(operatorHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("icon deve ser um dos valores suportados da shell."));
    }

    @Test
    @DisplayName("DELETE /api/v1/shell/catalog/navigation-items/{id} - should remove non-core area")
    void shouldDeleteNavigationItem() throws Exception {
        CreateShellNavigationItemRequest request = new CreateShellNavigationItemRequest(
                "ops-alerts-admin-it",
                "Alertas",
                "/ops-alerts-admin-it",
                "Alertas do workspace.",
                "inbox",
                "attention",
                "secondary",
                70,
                true,
                List.of("alertas")
        );

        mockMvc.perform(post("/v1/shell/catalog/navigation-items")
                        .with(operatorHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/v1/shell/catalog/navigation-items/ops-alerts-admin-it").with(operatorHeader()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/v1/shell/catalog").with(operatorHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id=='ops-alerts-admin-it')]").isEmpty());
    }

    @Test
    @DisplayName("DELETE /api/v1/shell/catalog/navigation-items/{id} - should reject core areas")
    void shouldRejectDeleteCoreNavigationItem() throws Exception {
        mockMvc.perform(delete("/v1/shell/catalog/navigation-items/home").with(operatorHeader()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Esta area faz parte da navegacao essencial e nao pode ser removida."));
    }

    @Test
    @DisplayName("PATCH /api/v1/shell/catalog/task-types/{taskType} - should persist task type changes")
    void shouldUpdateTaskType() throws Exception {
        UpdateShellTaskTypeRequest request = new UpdateShellTaskTypeRequest(
                "Pesquisar contexto",
                "Levantar sinais, fontes e contexto do trabalho.",
                15,
                true
        );

        mockMvc.perform(patch("/v1/shell/catalog/task-types/research")
                        .with(operatorHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.label").value("Pesquisar contexto"))
                .andExpect(jsonPath("$.sortOrder").value(15));

        mockMvc.perform(get("/v1/shell/navigation").with(operatorHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskTypes[0].label").value("Pesquisar contexto"));
    }

    @Test
    @DisplayName("POST /api/v1/shell/catalog/task-types - should create a task type")
    void shouldCreateTaskType() throws Exception {
        CreateShellTaskTypeRequest request = new CreateShellTaskTypeRequest(
                "triage-extra",
                "Triagem",
                "Classificar urgencia e proximo passo.",
                20,
                true
        );

        mockMvc.perform(post("/v1/shell/catalog/task-types")
                        .with(operatorHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.taskType").value("triage-extra"))
                .andExpect(jsonPath("$.label").value("Triagem"));
    }

    @Test
    @DisplayName("DELETE /api/v1/shell/catalog/task-types/{taskType} - should delete task type")
    void shouldDeleteTaskType() throws Exception {
        CreateShellTaskTypeRequest request = new CreateShellTaskTypeRequest(
                "triage-admin-it",
                "Triagem",
                "Classificar urgencia e proximo passo.",
                20,
                true
        );

        mockMvc.perform(post("/v1/shell/catalog/task-types")
                        .with(operatorHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/v1/shell/catalog/task-types/triage-admin-it").with(operatorHeader()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/v1/shell/catalog").with(operatorHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskTypes[?(@.taskType=='triage-admin-it')]").isEmpty());
    }

    private RequestPostProcessor operatorHeader() {
        Long operatorId = userRepository.findByEmail(OPERATOR_EMAIL).map(UserJpaEntity::getId).orElseThrow();
        return request -> {
            request.addHeader(WorkspaceContextService.HEADER_ACTOR_USER_ID, operatorId);
            return request;
        };
    }
}

