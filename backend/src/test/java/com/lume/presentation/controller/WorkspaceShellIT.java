package com.lume.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lume.infrastructure.persistence.entity.UserJpaEntity;
import com.lume.infrastructure.persistence.repository.JpaUserRepository;
import com.lume.workspace.dto.CreateAgentThreadRequest;
import com.lume.workspace.dto.CreateTaskRequest;
import com.lume.workspace.entity.UserPreferenceJpaEntity;
import com.lume.workspace.entity.WorkspaceJpaEntity;
import com.lume.workspace.repository.UserPreferenceJpaRepository;
import com.lume.workspace.repository.WorkspaceJpaRepository;
import com.lume.workspace.service.WorkspaceContextService;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Workspace Shell - Integration Tests")
class WorkspaceShellIT {

    private static final String OPERATOR_EMAIL = "operator@lume.local";
    private static final String PRIMARY_WORKSPACE_SLUG = "workspace-principal";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JpaUserRepository userRepository;

    @Autowired
    private WorkspaceJpaRepository workspaceRepository;

    @Autowired
    private UserPreferenceJpaRepository userPreferenceRepository;

    @BeforeEach
    void resetOperatorWorkspace() {
        UserJpaEntity operator = userRepository.findByEmail(OPERATOR_EMAIL).orElseThrow();
        WorkspaceJpaEntity primaryWorkspace = workspaceRepository.findBySlug(PRIMARY_WORKSPACE_SLUG).orElseThrow();

        operator.setOrganizationId(primaryWorkspace.getOrganizationId());
        operator.setWorkspaceId(primaryWorkspace.getId());
        userRepository.save(operator);

        UserPreferenceJpaEntity preference = userPreferenceRepository.findByUserId(operator.getId()).orElseGet(() -> {
            UserPreferenceJpaEntity entity = new UserPreferenceJpaEntity();
            entity.setUserId(operator.getId());
            return entity;
        });
        preference.setActiveWorkspaceId(primaryWorkspace.getId());
        userPreferenceRepository.save(preference);
    }

    @Test
    @Order(1)
    @DisplayName("GET /auth/session - should return organization and workspace context")
    void shouldReturnSessionContext() throws Exception {
        mockMvc.perform(get("/auth/session").with(operatorHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organization.slug").value("lume"))
                .andExpect(jsonPath("$.workspace.slug").value(PRIMARY_WORKSPACE_SLUG))
                .andExpect(jsonPath("$.role.code").value("workspace_admin"));
    }

    @Test
    @Order(2)
    @DisplayName("GET /workspace/summary - should return workspace summary")
    void shouldReturnWorkspaceSummary() throws Exception {
        mockMvc.perform(get("/workspace/summary").with(operatorHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workspaceName").value("Workspace Principal"))
                .andExpect(jsonPath("$.counts.libraryEntries").isNumber())
                .andExpect(jsonPath("$.counts.projects").isNumber())
                .andExpect(jsonPath("$.counts.tasks").isNumber())
                .andExpect(jsonPath("$.recentItems").isArray())
                .andExpect(jsonPath("$.workspaceFacets").isArray());
    }

    @Test
    @Order(3)
    @DisplayName("GET /library/entries - should list persisted documents")
    void shouldReturnLibraryEntries() throws Exception {
        mockMvc.perform(get("/library/entries").with(operatorHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].sourceLabel").value("Backend do workspace"));
    }

    @Test
    @Order(4)
    @DisplayName("GET /search - should search current workspace content")
    void shouldSearchAcrossWorkspace() throws Exception {
        mockMvc.perform(get("/search").with(operatorHeader()).param("q", "onboarding"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").exists());
    }

    @Test
    @Order(5)
    @DisplayName("POST /agents/threads - should create a persisted assisted thread")
    void shouldCreateAgentThread() throws Exception {
        CreateAgentThreadRequest request = new CreateAgentThreadRequest(
                "ops",
                "Mapeie um fluxo de onboarding com checkpoints de aprovacao."
        );

        mockMvc.perform(post("/agents/threads")
                        .with(operatorHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.thread.id").exists())
                .andExpect(jsonPath("$.thread.agentProfileId").value("ops"))
                .andExpect(jsonPath("$.messages.length()").value(2))
                .andExpect(jsonPath("$.messages[0].role").value("user"))
                .andExpect(jsonPath("$.messages[1].role").value("assistant"));
    }

    @Test
    @Order(6)
    @DisplayName("GET /projects - should list persisted projects")
    void shouldReturnProjects() throws Exception {
        mockMvc.perform(get("/projects").with(operatorHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].taskCount").isNumber());
    }

    @Test
    @Order(7)
    @DisplayName("POST /tasks - should create a persisted task with steps")
    void shouldCreateTask() throws Exception {
        CreateTaskRequest request = new CreateTaskRequest(
                "Crie um playbook operacional para onboarding e aprovacao.",
                "playbook",
                "proj-ops"
        );

        mockMvc.perform(post("/tasks")
                        .with(operatorHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.task.id").exists())
                .andExpect(jsonPath("$.task.taskType").value("playbook"))
                .andExpect(jsonPath("$.steps.length()").value(3));
    }

    @Test
    @Order(8)
    @DisplayName("GET /usage/summary - should return operational usage summary")
    void shouldReturnUsageSummary() throws Exception {
        mockMvc.perform(get("/usage/summary").with(operatorHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyCredits").value(300))
                .andExpect(jsonPath("$.unreadNotifications").isNumber());
    }

    @Test
    @Order(9)
    @DisplayName("GET /notifications - should list operational inbox")
    void shouldReturnNotifications() throws Exception {
        mockMvc.perform(get("/notifications").with(operatorHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").exists())
                .andExpect(jsonPath("$[0].path").exists());
    }

    @Test
    @Order(10)
    @DisplayName("GET /settings/overview - should return settings shell")
    void shouldReturnSettingsOverview() throws Exception {
        mockMvc.perform(get("/settings/overview").with(operatorHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationName").value("Lume"))
                .andExpect(jsonPath("$.sections").isArray())
                .andExpect(jsonPath("$.usage.dailyCredits").value(300));
    }

    private RequestPostProcessor operatorHeader() {
        Long operatorId = userRepository.findByEmail(OPERATOR_EMAIL).orElseThrow().getId();
        return request -> {
            request.addHeader(WorkspaceContextService.HEADER_ACTOR_USER_ID, operatorId);
            return request;
        };
    }
}
