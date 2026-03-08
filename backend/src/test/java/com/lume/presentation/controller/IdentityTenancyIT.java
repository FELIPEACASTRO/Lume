package com.lume.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lume.infrastructure.persistence.entity.UserJpaEntity;
import com.lume.infrastructure.persistence.repository.JpaUserRepository;
import com.lume.workspace.dto.CreateMemberRequest;
import com.lume.workspace.dto.UpdateMemberRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
                .andExpect(jsonPath("$.role.permissions[0]").value("workspace.read"));
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
                .andExpect(jsonPath("$.preferences.languageCode").value("en-US"));
    }
}
