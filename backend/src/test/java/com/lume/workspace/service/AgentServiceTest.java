package com.lume.workspace.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lume.workspace.dto.CreateAgentThreadRequest;
import com.lume.workspace.dto.UnifiedInferenceRequest;
import com.lume.workspace.dto.UnifiedInferenceResponse;
import com.lume.workspace.entity.AgentMessageJpaEntity;
import com.lume.workspace.entity.AgentProfileJpaEntity;
import com.lume.workspace.entity.AgentThreadJpaEntity;
import com.lume.workspace.inference.ProviderDefinition;
import com.lume.workspace.inference.error.AiProviderException;
import com.lume.workspace.inference.security.EnvironmentSecretResolver;
import com.lume.workspace.repository.AgentMessageJpaRepository;
import com.lume.workspace.repository.AgentProfileJpaRepository;
import com.lume.workspace.repository.AgentThreadJpaRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.env.MockEnvironment;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("AgentService - Unit Tests")
class AgentServiceTest {

    private AgentProfileJpaRepository agentProfileRepository;
    private AgentThreadJpaRepository agentThreadRepository;
    private AgentMessageJpaRepository agentMessageRepository;
    private SpyAuditLogService auditLogService;
    private StubProviderCatalogService providerCatalogService;
    private StubInferenceGatewayService inferenceGatewayService;
    private AgentService service;

    @BeforeEach
    void setUp() {
        agentProfileRepository = mock(AgentProfileJpaRepository.class);
        agentThreadRepository = mock(AgentThreadJpaRepository.class);
        agentMessageRepository = mock(AgentMessageJpaRepository.class);
        auditLogService = new SpyAuditLogService();
        providerCatalogService = new StubProviderCatalogService();
        inferenceGatewayService = new StubInferenceGatewayService(null);

        when(agentThreadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(agentMessageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service = new AgentService(
                agentProfileRepository,
                agentThreadRepository,
                agentMessageRepository,
                new StubWorkspaceContextService(),
                auditLogService,
                providerCatalogService,
                inferenceGatewayService,
                new NoOpWorkspaceLedgerService(),
                new StubWorkspaceOnboardingService()
        );
    }

    @Test
    @DisplayName("listThreads returns mapped thread responses with profile names")
    void shouldListThreadsWithProfileNames() {
        AgentProfileJpaEntity profile = defaultProfile();
        when(agentProfileRepository.findByWorkspaceIdOrderByNameAsc(1L)).thenReturn(List.of(profile));

        AgentThreadJpaEntity thread = defaultThread(profile.getId());
        when(agentThreadRepository.findByWorkspaceIdOrderByUpdatedAtDesc(eq(1L), any(PageRequest.class)))
                .thenReturn(List.of(thread));

        var result = service.listThreads();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).agentName()).isEqualTo("Assistente Lume");
        assertThat(result.get(0).runtimeState()).isEqualTo("completed");
    }

    @Test
    @DisplayName("createThread saves thread and returns conversation with assistant reply")
    void shouldCreateThreadSuccessfully() {
        AgentProfileJpaEntity profile = defaultProfile();
        when(agentProfileRepository.findByIdAndWorkspaceId("profile-1", 1L))
                .thenReturn(Optional.of(profile));

        inferenceGatewayService.response = new UnifiedInferenceResponse(
                "openai", "OpenAI", "gpt-4", "v1", "chat",
                true, true, false, "completed",
                "Resposta do assistente.", null,
                "openai", List.of("openai"), List.of(),
                500L, 100, 50, 0.002, "sync", "quality"
        );

        var request = new CreateAgentThreadRequest("profile-1", "Ola, como funciona?");
        var result = service.createThread(request);

        assertThat(result.thread().agentName()).isEqualTo("Assistente Lume");
        assertThat(result.messages()).hasSize(2);
        assertThat(result.messages().get(0).role()).isEqualTo("user");
        assertThat(result.messages().get(1).role()).isEqualTo("assistant");
        assertThat(auditLogService.recordedActions).contains("created");
        verify(agentThreadRepository, org.mockito.Mockito.atLeastOnce()).save(any());
    }

    @Test
    @DisplayName("createThread throws AiProviderException on inference failure")
    void shouldThrowOnInferenceFailure() {
        AgentProfileJpaEntity profile = defaultProfile();
        when(agentProfileRepository.findByIdAndWorkspaceId("profile-1", 1L))
                .thenReturn(Optional.of(profile));

        inferenceGatewayService.response = new UnifiedInferenceResponse(
                "openai", "OpenAI", "gpt-4", "v1", "chat",
                true, true, false, "failed",
                null, "Provider timeout",
                "openai", List.of("openai"), List.of(),
                null, null, null, null, "sync", "quality"
        );

        var request = new CreateAgentThreadRequest("profile-1", "Teste de falha");

        assertThatThrownBy(() -> service.createThread(request))
                .isInstanceOf(AiProviderException.class);
        assertThat(auditLogService.recordedActions).contains("failed");
    }

    @Test
    @DisplayName("listMessages returns messages in chronological order")
    void shouldListMessagesInOrder() {
        AgentThreadJpaEntity thread = defaultThread("profile-1");
        when(agentThreadRepository.findByIdAndWorkspaceId("thread-1", 1L))
                .thenReturn(Optional.of(thread));

        AgentMessageJpaEntity msg1 = buildMessage("msg-1", "thread-1", "user", "Ola");
        AgentMessageJpaEntity msg2 = buildMessage("msg-2", "thread-1", "assistant", "Oi!");
        when(agentMessageRepository.findByThreadIdOrderByCreatedAtAsc("thread-1"))
                .thenReturn(List.of(msg1, msg2));

        var result = service.listMessages("thread-1");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).role()).isEqualTo("user");
        assertThat(result.get(1).role()).isEqualTo("assistant");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private AgentProfileJpaEntity defaultProfile() {
        AgentProfileJpaEntity profile = new AgentProfileJpaEntity();
        profile.setId("profile-1");
        profile.setWorkspaceId(1L);
        profile.setName("Assistente Lume");
        profile.setSpecialty("Suporte geral");
        profile.setDescription("Assistente de suporte.");
        profile.setStatusLabel("Ativo");
        profile.setAvailability("live");
        profile.setNote("Pronto para uso.");
        profile.setProviderCode("openai");
        profile.setModelCode("gpt-4");
        profile.setVersionLabel("v1");
        profile.setSystemPrompt("Voce e um assistente.");
        return profile;
    }

    private AgentThreadJpaEntity defaultThread(String profileId) {
        AgentThreadJpaEntity thread = new AgentThreadJpaEntity();
        thread.setId("thread-1");
        thread.setWorkspaceId(1L);
        thread.setAgentProfileId(profileId);
        thread.setTitle("Conversa teste");
        thread.setStatusLabel("Concluida");
        thread.setAvailability("live");
        thread.setRuntimeState("completed");
        thread.setLastMessagePreview("Ola");
        triggerPrePersist(thread);
        return thread;
    }

    private AgentMessageJpaEntity buildMessage(String id, String threadId, String role, String body) {
        AgentMessageJpaEntity msg = new AgentMessageJpaEntity();
        msg.setId(id);
        msg.setThreadId(threadId);
        msg.setRole(role);
        msg.setBody(body);
        triggerPrePersist(msg);
        return msg;
    }

    private void triggerPrePersist(Object entity) {
        try {
            var method = entity.getClass().getDeclaredMethod("onCreate");
            method.setAccessible(true);
            method.invoke(entity);
        } catch (Exception ignored) {}
    }

    // ── test doubles ────────────────────────────────────────────────────────

    private static final class StubWorkspaceContextService extends WorkspaceContextService {
        private StubWorkspaceContextService() {
            super(null, null, null, null, null, null, new ObjectProvider<>() {
                @Override public HttpServletRequest getObject(Object... args) { return null; }
                @Override public HttpServletRequest getIfAvailable() { return null; }
                @Override public HttpServletRequest getIfUnique() { return null; }
                @Override public HttpServletRequest getObject() { return null; }
            });
        }

        @Override public void requirePermission(String permission) { }
        @Override public Long getWorkspaceId() { return 1L; }
        @Override public Long getActorUserIdOrNull() { return 1L; }
        @Override public String getActorName() { return "Lume Operator"; }
    }

    private static final class SpyAuditLogService extends AuditLogService {
        final List<String> recordedActions = new ArrayList<>();

        private SpyAuditLogService() {
            super(null, null, new ObjectMapper());
        }

        @Override
        public void record(String entityType, String entityId, String action, Object payload) {
            recordedActions.add(action);
        }

        @Override
        public void recordExplicit(Long orgId, Long wsId, Long userId,
                                   String entityType, String entityId, String action, Object payload) {
            recordedActions.add(action);
        }
    }

    private static final class StubProviderCatalogService extends ProviderCatalogService {
        private StubProviderCatalogService() {
            super(new EnvironmentSecretResolver(new MockEnvironment()));
        }

        @Override
        public Optional<ProviderDefinition> findProvider(String code) {
            return Optional.empty();
        }

        @Override
        public boolean isConfigured(ProviderDefinition provider) {
            return true;
        }

        @Override
        public List<String> missingCredentialEnvVars(ProviderDefinition provider) {
            return List.of();
        }
    }

    private static final class StubInferenceGatewayService extends InferenceGatewayService {
        UnifiedInferenceResponse response;

        private StubInferenceGatewayService(UnifiedInferenceResponse response) {
            super(null);
            this.response = response;
        }

        @Override
        public UnifiedInferenceResponse execute(UnifiedInferenceRequest request) {
            return response;
        }
    }

    private static final class StubWorkspaceOnboardingService extends WorkspaceOnboardingService {
        private StubWorkspaceOnboardingService() {
            super(null, null, null, null, null);
        }

        @Override
        public com.lume.workspace.dto.WorkspaceOnboardingResponse advanceCurrentOnboarding(
                String targetStep, String note, String eventContext) {
            return null;
        }
    }
}
