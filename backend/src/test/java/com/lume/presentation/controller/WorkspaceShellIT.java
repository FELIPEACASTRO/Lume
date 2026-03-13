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
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;
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
    @DisplayName("GET /api/v1/home/overview - should return persisted home overview state")
    void shouldReturnHomeOverview() throws Exception {
        mockMvc.perform(get("/v1/home/overview").with(operatorHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headline").isString())
                .andExpect(jsonPath("$.supportingText").isString())
                .andExpect(jsonPath("$.blocks").isArray())
                .andExpect(jsonPath("$.blocks[0].maxItems").isNumber());
    }

    @Test
    @Order(4)
    @DisplayName("GET /library/entries - should list persisted documents")
    void shouldReturnLibraryEntries() throws Exception {
        mockMvc.perform(get("/library/entries").with(operatorHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].sourceLabel").value("Backend do workspace"))
                .andExpect(jsonPath("$[0].entryType").value("artifact"))
                .andExpect(jsonPath("$[0].versionCount").isNumber());
    }

    @Test
    @Order(5)
    @DisplayName("GET /search - should search current workspace content")
    void shouldSearchAcrossWorkspace() throws Exception {
        mockMvc.perform(get("/search").with(operatorHeader()).param("q", "onboarding"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").exists());
    }

    @Test
    @Order(6)
    @DisplayName("POST /agents/threads - should return an operational provider error when runtime is unavailable")
    void shouldCreateAgentThread() throws Exception {
        CreateAgentThreadRequest request = new CreateAgentThreadRequest(
                "ops",
                "Mapeie um fluxo de onboarding com checkpoints de aprovacao."
        );

        mockMvc.perform(post("/agents/threads")
                        .with(operatorHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @Order(7)
    @DisplayName("GET /projects - should list persisted projects")
    void shouldReturnProjects() throws Exception {
        mockMvc.perform(get("/projects").with(operatorHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].taskCount").isNumber());
    }

    @Test
    @Order(8)
    @DisplayName("POST /tasks - should create a persisted task with truthful initial state")
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
                .andExpect(jsonPath("$.task.runtimeState").value("queued"))
                .andExpect(jsonPath("$.steps.length()").value(0));
    }

    @Test
    @Order(9)
    @DisplayName("POST /tasks - should persist provider, model and version metadata when runtime is selected")
    void shouldCreateTaskWithRuntimeMetadata() throws Exception {
        CreateTaskRequest request = new CreateTaskRequest(
                "Monte um plano de resposta para incidentes com checkpoints de aprovacao.",
                "playbook",
                "proj-ops",
                "openai",
                "openai:gpt-4.1-mini",
                "agent-v1-openai"
        );

        mockMvc.perform(post("/tasks")
                        .with(operatorHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.task.providerCode").value("openai"))
                .andExpect(jsonPath("$.task.modelCode").value("openai:gpt-4.1-mini"))
                .andExpect(jsonPath("$.task.versionLabel").value("agent-v1-openai"));
    }

    @Test
    @Order(10)
    @DisplayName("POST /tasks - should reject model that does not belong to provider")
    void shouldRejectTaskRuntimeWithModelFromAnotherProvider() throws Exception {
        CreateTaskRequest request = new CreateTaskRequest(
                "Teste de runtime invalido para tarefa.",
                "research",
                "proj-ops",
                "openai",
                "anthropic:claude-sonnet-4-5",
                "agent-v1-claude"
        );

        mockMvc.perform(post("/tasks")
                        .with(operatorHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("O modelo selecionado nao pertence ao provedor informado."));
    }

    @Test
    @Order(11)
    @DisplayName("GET /api/v1/search/results - should return grouped real results")
    void shouldReturnSearchResults() throws Exception {
        mockMvc.perform(get("/v1/search/results").with(operatorHeader()).param("q", "onboarding"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("onboarding"))
                .andExpect(jsonPath("$.totalResults").isNumber())
                .andExpect(jsonPath("$.results").isArray());
    }

    @Test
    @Order(12)
    @DisplayName("GET /usage/summary - should return operational usage summary")
    void shouldReturnUsageSummary() throws Exception {
        mockMvc.perform(get("/usage/summary").with(operatorHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyCredits").value(300))
                .andExpect(jsonPath("$.unreadNotifications").isNumber())
                .andExpect(jsonPath("$.budget.costCenter").value("core_now"))
                .andExpect(jsonPath("$.commercial.planCode").value("starter"))
                .andExpect(jsonPath("$.commercial.includedCredits").value(500))
                .andExpect(jsonPath("$.budget.chargebackMode").value("showback"))
                .andExpect(jsonPath("$.budget.softLimitCredits").isNumber())
                .andExpect(jsonPath("$.budget.hardLimitCredits").isNumber());
    }

    @Test
    @Order(13)
    @DisplayName("GET /notifications - should list operational inbox")
    void shouldReturnNotifications() throws Exception {
        mockMvc.perform(get("/notifications").with(operatorHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").exists())
                .andExpect(jsonPath("$[0].path").exists());
    }

    @Test
    @Order(14)
    @DisplayName("GET /settings/overview - should return settings shell")
    void shouldReturnSettingsOverview() throws Exception {
        mockMvc.perform(get("/settings/overview").with(operatorHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationName").value("Lume"))
                .andExpect(jsonPath("$.sections").isArray())
                .andExpect(jsonPath("$.usage.dailyCredits").value(300))
                .andExpect(jsonPath("$.commercial.planLabel").value("Starter"))
                .andExpect(jsonPath("$.governance.openSupportTickets").isNumber())
                .andExpect(jsonPath("$.governance.overdueSupportTickets").isNumber())
                .andExpect(jsonPath("$.governance.coreLiveProviders").isNumber())
                .andExpect(jsonPath("$.compliance.billingWebhookSecretConfigured").isBoolean())
                .andExpect(jsonPath("$.compliance.auditTrailEnabled").value(true))
                .andExpect(jsonPath("$.compliance.retentionPolicyStatus").isString())
                .andExpect(jsonPath("$.compliance.consentTrackingEnabled").isBoolean())
                .andExpect(jsonPath("$.sections[?(@.key=='knowledge')]").exists())
                .andExpect(jsonPath("$.sections[?(@.key=='finops')]").exists())
                .andExpect(jsonPath("$.usage.budget.costCenter").value("core_now"));

        mockMvc.perform(get("/v1/settings/overview").with(operatorHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationName").value("Lume"))
                .andExpect(jsonPath("$.sections[?(@.key=='providers-runtime')]").exists())
                .andExpect(jsonPath("$.governance.note").isString())
                .andExpect(jsonPath("$.compliance.note").isString());
    }

    @Test
    @Order(15)
    @DisplayName("GET /api/v1/knowledge-sources - should list persisted knowledge sources")
    void shouldReturnKnowledgeSources() throws Exception {
        mockMvc.perform(get("/v1/knowledge-sources").with(operatorHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].title").exists())
                .andExpect(jsonPath("$[0].enabledForAgents").isBoolean())
                .andExpect(jsonPath("$[0].documentCount").isNumber())
                .andExpect(jsonPath("$[*].statusLabel", not(hasItem("Preview assistido"))));
    }

    @Test
    @Order(16)
    @DisplayName("GET /api/v1/library/entries/{id}/versions - should return persisted artifact versions")
    void shouldReturnArtifactVersions() throws Exception {
        mockMvc.perform(get("/v1/library/entries/{id}/versions", "lib-onboarding").with(operatorHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].entryId").value("lib-onboarding"))
                .andExpect(jsonPath("$[0].versionLabel").exists());
    }

    @Test
    @Order(17)
    @DisplayName("GET /api/v1/prompt-templates - should return persisted prompt templates")
    void shouldReturnPromptTemplates() throws Exception {
        mockMvc.perform(get("/v1/prompt-templates").with(operatorHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].promptBody").exists())
                .andExpect(jsonPath("$[0].variables").isArray());
    }

    private RequestPostProcessor operatorHeader() {
        Long operatorId = userRepository.findByEmail(OPERATOR_EMAIL).orElseThrow().getId();
        return request -> {
            request.addHeader(WorkspaceContextService.HEADER_ACTOR_USER_ID, operatorId);
            return request;
        };
    }
}

