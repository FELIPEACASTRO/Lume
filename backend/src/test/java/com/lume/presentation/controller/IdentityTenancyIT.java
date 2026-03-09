package com.lume.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lume.infrastructure.persistence.entity.UserJpaEntity;
import com.lume.infrastructure.persistence.repository.JpaUserRepository;
import com.lume.workspace.dto.CreateMemberRequest;
import com.lume.workspace.dto.CreateArtifactVersionRequest;
import com.lume.workspace.dto.CreateKnowledgeSourceRequest;
import com.lume.workspace.dto.CreatePromptTemplateRequest;
import com.lume.workspace.dto.UpdateAgentRuntimeRequest;
import com.lume.workspace.dto.UpdateWorkspaceBudgetRequest;
import com.lume.workspace.dto.UpdateKnowledgeSourceRequest;
import com.lume.workspace.dto.UpdateMemberRequest;
import com.lume.workspace.dto.UpdatePromptTemplateRequest;
import com.lume.workspace.dto.UpdateSettingsPreferencesRequest;
import com.lume.workspace.entity.MembershipJpaEntity;
import com.lume.workspace.entity.WorkspaceJpaEntity;
import com.lume.workspace.repository.MembershipJpaRepository;
import com.lume.workspace.repository.WorkspaceJpaRepository;
import com.lume.workspace.service.WorkspaceContextService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.hasItems;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Identity and Tenancy - Contratos versionados")
class IdentityTenancyIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JpaUserRepository userRepository;

    @Autowired
    private WorkspaceJpaRepository workspaceRepository;

    @Autowired
    private MembershipJpaRepository membershipRepository;

    @Test
    @DisplayName("GET /api/v1/auth/session - Deve expor role com permissoes")
    void shouldReturnVersionedSession() throws Exception {
        mockMvc.perform(get("/api/v1/auth/session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workspace.slug").value("workspace-principal"))
                .andExpect(jsonPath("$.role.code").value("workspace_admin"))
                .andExpect(jsonPath("$.role.permissions", hasItems(
                        "workspace.read",
                        "providers.read",
                        "agents.runtime.manage",
                        "research.run",
                        "threat_intel.read"
                )));
    }

    @Test
    @DisplayName("GET /api/v1/workspaces - Deve listar workspaces acessiveis")
    void shouldListAvailableWorkspaces() throws Exception {
        mockMvc.perform(get("/api/v1/workspaces"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].roleCode").exists());
    }

    @Test
    @DisplayName("POST /api/v1/workspaces/{id}/activate - Deve trocar o workspace ativo")
    void shouldActivateWorkspace() throws Exception {
        WorkspaceJpaEntity strategy = workspaceRepository.findBySlug("workspace-strategy").orElseThrow();
        WorkspaceJpaEntity primary = workspaceRepository.findBySlug("workspace-principal").orElseThrow();

        mockMvc.perform(post("/api/v1/workspaces/{id}/activate", strategy.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/auth/session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workspace.slug").value("workspace-strategy"));

        mockMvc.perform(get("/workspace/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workspaceName").value("Workspace Strategy"));

        mockMvc.perform(post("/api/v1/workspaces/{id}/activate", primary.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/v1/members - Admin deve listar membros do workspace ativo")
    void shouldListMembersForAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].roleCode").exists());
    }

    @Test
    @DisplayName("GET /api/v1/members - Membro comum deve receber 403")
    void shouldDenyMemberListingForWorkspaceMember() throws Exception {
        UserJpaEntity analyst = userRepository.findByEmail("ana.strategy@lume.local").orElseThrow();

        mockMvc.perform(get("/api/v1/members")
                        .header(WorkspaceContextService.HEADER_ACTOR_USER_ID, analyst.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST/PATCH /api/v1/members - Admin deve criar e atualizar membership")
    void shouldCreateAndUpdateMember() throws Exception {
        CreateMemberRequest createRequest = new CreateMemberRequest(
                "Workspace Reviewer",
                "reviewer@lume.local",
                "secret123",
                "workspace_member"
        );

        mockMvc.perform(post("/api/v1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("reviewer@lume.local"))
                .andExpect(jsonPath("$.roleCode").value("workspace_member"));

        UserJpaEntity reviewer = userRepository.findByEmail("reviewer@lume.local").orElseThrow();
        MembershipJpaEntity membership = membershipRepository.findByUserIdAndWorkspaceId(reviewer.getId(), reviewer.getWorkspaceId()).orElseThrow();

        UpdateMemberRequest updateRequest = new UpdateMemberRequest(
                "Workspace Reviewer",
                "reviewer@lume.local",
                "",
                "workspace_admin",
                true
        );

        mockMvc.perform(patch("/api/v1/members/{id}", membership.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roleCode").value("workspace_admin"));
    }

    @Test
    @DisplayName("GET /api/v1/settings/preferences - Deve retornar preferencias normalizadas")
    void shouldReturnSettingsPreferences() throws Exception {
        mockMvc.perform(get("/api/v1/settings/preferences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appearance").value("light"))
                .andExpect(jsonPath("$.languageCode").value("pt-BR"))
                .andExpect(jsonPath("$.emailUpdates").value(true))
                .andExpect(jsonPath("$.productUpdates").value(true));
    }

    @Test
    @DisplayName("PATCH /api/v1/settings/preferences - Deve atualizar preferencias reais do usuario")
    void shouldUpdateSettingsPreferences() throws Exception {
        UpdateSettingsPreferencesRequest request = new UpdateSettingsPreferencesRequest(
                "dark",
                "en-US",
                false,
                true
        );

        mockMvc.perform(patch("/api/v1/settings/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appearance").value("dark"))
                .andExpect(jsonPath("$.languageCode").value("en-US"))
                .andExpect(jsonPath("$.emailUpdates").value(false))
                .andExpect(jsonPath("$.productUpdates").value(true));

        mockMvc.perform(get("/settings/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preferences.appearance").value("dark"))
                .andExpect(jsonPath("$.preferences.languageCode").value("en-US"))
                .andExpect(jsonPath("$.sections[?(@.key=='knowledge')]").exists())
                .andExpect(jsonPath("$.sections[?(@.key=='finops')]").exists())
                .andExpect(jsonPath("$.sections[?(@.key=='providers-runtime')]").exists())
                .andExpect(jsonPath("$.sections[?(@.key=='threat-intelligence')]").exists());

        mockMvc.perform(get("/api/v1/auth/session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role.permissions", hasItems("artifacts.read", "templates.read")));
    }

    @Test
    @DisplayName("GET/PATCH /api/v1/budgets/current - Admin deve ler e atualizar budget do workspace")
    void shouldReadAndUpdateWorkspaceBudget() throws Exception {
        mockMvc.perform(get("/api/v1/budgets/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.costCenter").value("core_now"))
                .andExpect(jsonPath("$.chargebackMode").value("showback"));

        UpdateWorkspaceBudgetRequest request = new UpdateWorkspaceBudgetRequest(
                "finops-brasil",
                "chargeback",
                280,
                420
        );

        mockMvc.perform(patch("/api/v1/budgets/current")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.costCenter").value("finops-brasil"))
                .andExpect(jsonPath("$.chargebackMode").value("chargeback"))
                .andExpect(jsonPath("$.softLimitCredits").value(280))
                .andExpect(jsonPath("$.hardLimitCredits").value(420));
    }

    @Test
    @DisplayName("PATCH /api/v1/budgets/current - Membro comum nao deve alterar budget")
    void shouldDenyBudgetUpdateForWorkspaceMember() throws Exception {
        UserJpaEntity analyst = userRepository.findByEmail("ana.strategy@lume.local").orElseThrow();
        UpdateWorkspaceBudgetRequest request = new UpdateWorkspaceBudgetRequest(
                "member-scope",
                "showback",
                250,
                400
        );

        mockMvc.perform(patch("/api/v1/budgets/current")
                        .header(WorkspaceContextService.HEADER_ACTOR_USER_ID, analyst.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /api/v1/agents/profiles/{id}/runtime - Admin deve atualizar runtime do agente")
    void shouldUpdateAgentRuntimeForAdmin() throws Exception {
        UpdateAgentRuntimeRequest request = new UpdateAgentRuntimeRequest(
                "anthropic",
                "anthropic:claude-sonnet-4-5",
                "agent-v2-claude",
                "Responda com foco em compliance e evidencia."
        );

        mockMvc.perform(patch("/api/v1/agents/profiles/{id}/runtime", "ops")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("ops"))
                .andExpect(jsonPath("$.providerCode").value("anthropic"))
                .andExpect(jsonPath("$.modelCode").value("anthropic:claude-sonnet-4-5"))
                .andExpect(jsonPath("$.versionLabel").value("agent-v2-claude"))
                .andExpect(jsonPath("$.apiStyle").value("messages"));
    }

    @Test
    @DisplayName("GET /api/v1/threat-intel/providers - Membro comum nao deve enxergar threat-intel")
    void shouldDenyThreatIntelListingForWorkspaceMember() throws Exception {
        UserJpaEntity analyst = userRepository.findByEmail("ana.strategy@lume.local").orElseThrow();

        mockMvc.perform(get("/api/v1/threat-intel/providers")
                        .header(WorkspaceContextService.HEADER_ACTOR_USER_ID, analyst.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET/POST/PATCH/DELETE /api/v1/knowledge-sources - Admin deve gerenciar fontes de conhecimento")
    void shouldManageKnowledgeSourcesForAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/knowledge-sources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists());

        CreateKnowledgeSourceRequest createRequest = new CreateKnowledgeSourceRequest(
                "Repositorio de playbooks",
                "repository",
                "proj-ops",
                "https://github.com/lume/playbooks",
                8,
                true,
                "Indexado",
                "live",
                "Repositorio governado pelo time de operacao."
        );

        String createdId = mockMvc.perform(post("/api/v1/knowledge-sources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Repositorio de playbooks"))
                .andExpect(jsonPath("$.projectId").value("proj-ops"))
                .andExpect(jsonPath("$.documentCount").value(8))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String sourceId = objectMapper.readTree(createdId).path("id").asText();

        UpdateKnowledgeSourceRequest updateRequest = new UpdateKnowledgeSourceRequest(
                "Repositorio de playbooks atualizado",
                null,
                "proj-ops",
                "https://github.com/lume/playbooks-v2",
                11,
                false,
                "Reindexado",
                "live",
                "Repositorio governado e pronto para retrieval."
        );

        mockMvc.perform(patch("/api/v1/knowledge-sources/{id}", sourceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Repositorio de playbooks atualizado"))
                .andExpect(jsonPath("$.enabledForAgents").value(false))
                .andExpect(jsonPath("$.documentCount").value(11));

        mockMvc.perform(delete("/api/v1/knowledge-sources/{id}", sourceId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Workspace member - deve ler knowledge, mas nao criar ou alterar")
    void shouldDenyKnowledgeManagementForWorkspaceMember() throws Exception {
        UserJpaEntity analyst = userRepository.findByEmail("ana.strategy@lume.local").orElseThrow();

        mockMvc.perform(get("/api/v1/knowledge-sources")
                        .header(WorkspaceContextService.HEADER_ACTOR_USER_ID, analyst.getId()))
                .andExpect(status().isOk());

        CreateKnowledgeSourceRequest createRequest = new CreateKnowledgeSourceRequest(
                "Fonte bloqueada",
                "library",
                null,
                "lume://library/bloqueada",
                1,
                true,
                "Indexado",
                "live",
                "Tentativa sem permissao."
        );

        mockMvc.perform(post("/api/v1/knowledge-sources")
                        .header(WorkspaceContextService.HEADER_ACTOR_USER_ID, analyst.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET/POST /api/v1/library/entries/{id}/versions - admin cria versoes e membro so le")
    void shouldManageArtifactVersionsWithRbac() throws Exception {
        mockMvc.perform(get("/api/v1/library/entries/{id}/versions", "lib-onboarding"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].versionLabel").exists());

        CreateArtifactVersionRequest request = new CreateArtifactVersionRequest(
                "v3",
                "Checklist de rollout incorporado",
                "Inclui handoff para growth, etapa de validacao e criterio de rollback."
        );

        mockMvc.perform(post("/api/v1/library/entries/{id}/versions", "lib-onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.versionLabel").value("v3"));

        CreateMemberRequest createMemberRequest = new CreateMemberRequest(
                "Artifact Reader",
                "artifact.reader@lume.local",
                "secret123",
                "workspace_member"
        );

        mockMvc.perform(post("/api/v1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createMemberRequest)))
                .andExpect(status().isCreated());

        UserJpaEntity analyst = userRepository.findByEmail("artifact.reader@lume.local").orElseThrow();
        mockMvc.perform(get("/api/v1/library/entries/{id}/versions", "lib-onboarding")
                        .header(WorkspaceContextService.HEADER_ACTOR_USER_ID, analyst.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/library/entries/{id}/versions", "lib-onboarding")
                        .header(WorkspaceContextService.HEADER_ACTOR_USER_ID, analyst.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET/POST/PATCH/DELETE /api/v1/prompt-templates - admin gerencia templates e membro nao cria")
    void shouldManagePromptTemplatesWithRbac() throws Exception {
        mockMvc.perform(get("/api/v1/prompt-templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].promptBody").exists());

        CreatePromptTemplateRequest createRequest = new CreatePromptTemplateRequest(
                "Brief de rollout",
                "Template para preparar tese, risco e owner.",
                "Prepare o rollout de {{produto}} com dono, risco, aprovacao e rollback.",
                "project",
                "proj-ops",
                "ops",
                java.util.List.of("produto", "owner", "rollback"),
                true,
                null,
                null
        );

        String createdResponse = mockMvc.perform(post("/api/v1/prompt-templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Brief de rollout"))
                .andExpect(jsonPath("$.variables[0]").value("produto"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String templateId = objectMapper.readTree(createdResponse).path("id").asText();

        UpdatePromptTemplateRequest updateRequest = new UpdatePromptTemplateRequest(
                null,
                null,
                null,
                null,
                null,
                null,
                java.util.List.of("produto", "owner", "rollback", "aceite"),
                false,
                null,
                null
        );

        mockMvc.perform(patch("/api/v1/prompt-templates/{id}", templateId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favorited").value(false))
                .andExpect(jsonPath("$.variables[3]").value("aceite"));

        mockMvc.perform(post("/api/v1/prompt-templates/{id}/touch", templateId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastUsedAt").isNotEmpty());

        mockMvc.perform(delete("/api/v1/prompt-templates/{id}", templateId))
                .andExpect(status().isNoContent());

        UserJpaEntity analyst = userRepository.findByEmail("ana.strategy@lume.local").orElseThrow();
        mockMvc.perform(post("/api/v1/prompt-templates")
                        .header(WorkspaceContextService.HEADER_ACTOR_USER_ID, analyst.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isForbidden());
    }
}
