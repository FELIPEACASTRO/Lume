package com.lume.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lume.infrastructure.persistence.entity.UserJpaEntity;
import com.lume.infrastructure.persistence.repository.JpaUserRepository;
import com.lume.workspace.dto.CreateMemberRequest;
import com.lume.workspace.dto.CreateArtifactVersionRequest;
import com.lume.workspace.dto.CreateKnowledgeSourceRequest;
import com.lume.workspace.dto.CreatePromptTemplateRequest;
import com.lume.workspace.dto.BillingWebhookEventRequest;
import com.lume.workspace.dto.FinopsReconciliationRunRequest;
import com.lume.workspace.dto.UpdateAgentRuntimeRequest;
import com.lume.workspace.dto.UpdateWorkspaceOnboardingRequest;
import com.lume.workspace.dto.UpdateWorkspaceSubscriptionRequest;
import com.lume.workspace.dto.UpdateWorkspaceBudgetRequest;
import com.lume.workspace.dto.UpdateKnowledgeSourceRequest;
import com.lume.workspace.dto.UpdateMemberRequest;
import com.lume.workspace.dto.UpdatePromptTemplateRequest;
import com.lume.workspace.dto.UpdateSettingsPreferencesRequest;
import com.lume.workspace.entity.MembershipJpaEntity;
import com.lume.workspace.entity.WorkspaceJpaEntity;
import com.lume.workspace.entity.WorkspaceSubscriptionJpaEntity;
import com.lume.workspace.repository.MembershipJpaRepository;
import com.lume.workspace.repository.WorkspaceJpaRepository;
import com.lume.workspace.repository.WorkspaceSubscriptionJpaRepository;
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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

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

    @Autowired
    private WorkspaceSubscriptionJpaRepository subscriptionRepository;

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
    @DisplayName("GET/PATCH /api/v1/onboarding/current - Deve ler e atualizar onboarding do workspace")
    void shouldReadAndUpdateWorkspaceOnboarding() throws Exception {
        mockMvc.perform(get("/api/v1/onboarding/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryUseCase").isString())
                .andExpect(jsonPath("$.workStyle").isString());

        UpdateWorkspaceOnboardingRequest request = new UpdateWorkspaceOnboardingRequest(
                "analysis",
                "department_team",
                "active",
                "Workspace em operacao com foco em analise recorrente."
        );

        mockMvc.perform(patch("/api/v1/onboarding/current")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryUseCase").value("analysis"))
                .andExpect(jsonPath("$.workStyle").value("department_team"))
                .andExpect(jsonPath("$.activationStatus").value("active"));
    }

    @Test
    @DisplayName("GET/PATCH /api/v1/billing/subscription - Deve ler e atualizar plano comercial")
    void shouldReadAndUpdateWorkspaceSubscription() throws Exception {
        mockMvc.perform(get("/api/v1/billing/subscription"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planCode").isString())
                .andExpect(jsonPath("$.includedCredits").isNumber());

        UpdateWorkspaceSubscriptionRequest request = new UpdateWorkspaceSubscriptionRequest(
                "pro",
                "active",
                "monthly",
                5000,
                400,
                null,
                "Workspace promovido para uso recorrente com credito extra."
        );

        mockMvc.perform(patch("/api/v1/billing/subscription")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planCode").value("pro"))
                .andExpect(jsonPath("$.includedCredits").value(5000))
                .andExpect(jsonPath("$.extraCredits").value(400))
                .andExpect(jsonPath("$.totalCredits").value(5400));
    }

    @Test
    @DisplayName("POST /api/v1/billing/webhooks/provider-event - Deve processar webhook com idempotencia")
    void shouldProcessBillingWebhookIdempotently() throws Exception {
        Long workspaceId = workspaceRepository.findBySlug("workspace-principal").orElseThrow().getId();
        BillingWebhookEventRequest request = new BillingWebhookEventRequest(
                workspaceId,
                "evt-billing-001",
                "invoice.paid",
                "paid",
                "INV-WEBHOOK-001",
                new BigDecimal("99.90"),
                "BRL",
                "Pagamento da assinatura mensal",
                "2026-04-05",
                "2026-03-09T22:15:00",
                "core",
                "active",
                "monthly",
                1500,
                200,
                "2026-04-09",
                "Atualizacao via webhook de billing.",
                120
        );

        String signature = hmacSha256Hex(
                "test-billing-webhook-secret",
                canonicalSignaturePayload(request)
        );

        mockMvc.perform(post("/api/v1/billing/webhooks/provider-event")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Lume-Billing-Signature", signature)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processingStatus").value("processed"))
                .andExpect(jsonPath("$.duplicate").value(false))
                .andExpect(jsonPath("$.invoiceNumber").value("INV-WEBHOOK-001"));

        mockMvc.perform(post("/api/v1/billing/webhooks/provider-event")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Lume-Billing-Signature", signature)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processingStatus").value("duplicate"))
                .andExpect(jsonPath("$.duplicate").value(true))
                .andExpect(jsonPath("$.invoiceNumber").value("INV-WEBHOOK-001"));

        mockMvc.perform(get("/api/v1/billing/subscription"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planCode").value("core"))
                .andExpect(jsonPath("$.includedCredits").value(1500))
                .andExpect(jsonPath("$.extraCredits").value(320))
                .andExpect(jsonPath("$.totalCredits").value(1820));

        mockMvc.perform(get("/api/v1/billing/invoices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].invoiceNumber").value("INV-WEBHOOK-001"))
                .andExpect(jsonPath("$[0].status").value("paid"));

        mockMvc.perform(get("/api/v1/billing/payment-events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].gatewayEventId").value("evt-billing-001"))
                .andExpect(jsonPath("$[0].eventType").value("invoice_paid"))
                .andExpect(jsonPath("$[0].status").value("processed"));
    }

    @Test
    @DisplayName("POST /api/v1/billing/webhooks/provider-event - Deve negar assinatura invalida")
    void shouldRejectBillingWebhookWithInvalidSignature() throws Exception {
        Long workspaceId = workspaceRepository.findBySlug("workspace-principal").orElseThrow().getId();
        BillingWebhookEventRequest request = new BillingWebhookEventRequest(
                workspaceId,
                "evt-billing-invalid-signature",
                "invoice.paid",
                "paid",
                "INV-WEBHOOK-INVALID",
                new BigDecimal("49.90"),
                "BRL",
                "Assinatura invalida",
                "2026-04-05",
                "2026-03-09T22:30:00",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/billing/webhooks/provider-event")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Lume-Billing-Signature", "invalid-signature")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Assinatura do webhook invalida."));
    }

    @Test
    @DisplayName("GET /api/v1/finops/reconciliation - Deve retornar preview de reconciliacao")
    void shouldPreviewFinopsReconciliation() throws Exception {
        mockMvc.perform(get("/api/v1/finops/reconciliation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reconciliationStatus").isString())
                .andExpect(jsonPath("$.subscriptionCredits").isNumber())
                .andExpect(jsonPath("$.ledgerCreditBalance").isNumber())
                .andExpect(jsonPath("$.creditDrift").isNumber())
                .andExpect(jsonPath("$.creditFixApplied").value(false))
                .andExpect(jsonPath("$.generatedAt").isString());
    }

    @Test
    @DisplayName("POST /api/v1/finops/reconciliation/run - Deve aplicar ajuste de creditos quando houver drift")
    void shouldRunFinopsReconciliationWithCreditFix() throws Exception {
        mockMvc.perform(post("/api/v1/finops/reconciliation/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FinopsReconciliationRunRequest(true))))
                .andExpect(status().isOk());

        Long workspaceId = workspaceRepository.findBySlug("workspace-principal").orElseThrow().getId();
        WorkspaceSubscriptionJpaEntity subscription = subscriptionRepository.findByWorkspaceId(workspaceId).orElseThrow();
        subscription.setExtraCredits(subscription.getExtraCredits() + 37);
        subscriptionRepository.save(subscription);

        mockMvc.perform(post("/api/v1/finops/reconciliation/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FinopsReconciliationRunRequest(true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reconciliationStatus").value("balanced"))
                .andExpect(jsonPath("$.creditFixApplied").value(true))
                .andExpect(jsonPath("$.creditFixDelta").value(37))
                .andExpect(jsonPath("$.creditDrift").value(0));
    }

    @Test
    @DisplayName("GET /api/v1/finops/reconciliation/history - Deve retornar historico das execucoes de reconciliacao")
    void shouldReturnFinopsReconciliationHistory() throws Exception {
        mockMvc.perform(post("/api/v1/finops/reconciliation/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FinopsReconciliationRunRequest(false))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/finops/reconciliation/history")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].runMode").value("manual"))
                .andExpect(jsonPath("$[0].reconciliationStatus").isString())
                .andExpect(jsonPath("$[0].subscriptionCredits").isNumber())
                .andExpect(jsonPath("$[0].ledgerCreditBalance").isNumber())
                .andExpect(jsonPath("$[0].executedAt").isString());
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
                .andExpect(jsonPath("$.commercial.planCode").isString())
                .andExpect(jsonPath("$.sections[?(@.key=='knowledge')]").exists())
                .andExpect(jsonPath("$.sections[?(@.key=='finops')]").exists())
                .andExpect(jsonPath("$.sections[?(@.key=='providers-runtime')]").exists());

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

    private String canonicalSignaturePayload(BillingWebhookEventRequest request) {
        return String.join("|",
                safeCanonical(request.gatewayEventId()),
                String.valueOf(request.workspaceId()),
                safeCanonical(request.eventType()),
                safeCanonical(request.status()),
                safeCanonical(request.invoiceNumber()),
                request.amountBrl() == null ? "" : request.amountBrl().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(),
                safeCanonical(request.currency())
        );
    }

    private String safeCanonical(String value) {
        return value == null ? "" : value.trim();
    }

    private String hmacSha256Hex(String secret, String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            builder.append(String.format(Locale.ROOT, "%02x", b));
        }
        return builder.toString();
    }
}
