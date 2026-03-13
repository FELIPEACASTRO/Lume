package com.lume.workspace.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lume.workspace.dto.CreateTaskRequest;
import com.lume.workspace.dto.TaskDetailResponse;
import com.lume.workspace.dto.TaskSummaryResponse;
import com.lume.workspace.entity.ProjectJpaEntity;
import com.lume.workspace.entity.TaskJpaEntity;
import com.lume.workspace.entity.TaskStepJpaEntity;
import com.lume.workspace.inference.CredentialFieldDefinition;
import com.lume.workspace.inference.InferenceProtocol;
import com.lume.workspace.inference.ModelDefinition;
import com.lume.workspace.inference.ProviderDefinition;
import com.lume.workspace.inference.security.EnvironmentSecretResolver;
import com.lume.workspace.repository.ProjectJpaRepository;
import com.lume.workspace.repository.TaskJpaRepository;
import com.lume.workspace.repository.TaskStepJpaRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Pageable;
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

@DisplayName("TaskService - Unit Tests")
class TaskServiceTest {

    private TaskJpaRepository taskRepository;
    private TaskStepJpaRepository taskStepRepository;
    private ProjectJpaRepository projectRepository;
    private SpyAuditLogService auditLogService;
    private NoOpWorkspaceLedgerService ledgerService;
    private StubProviderCatalogService providerCatalogService;
    private TaskService service;

    @BeforeEach
    void setUp() {
        taskRepository = mock(TaskJpaRepository.class);
        taskStepRepository = mock(TaskStepJpaRepository.class);
        projectRepository = mock(ProjectJpaRepository.class);
        auditLogService = new SpyAuditLogService();
        ledgerService = new NoOpWorkspaceLedgerService();
        providerCatalogService = new StubProviderCatalogService();

        when(taskRepository.save(any())).thenAnswer(inv -> {
            TaskJpaEntity entity = inv.getArgument(0);
            triggerPrePersist(entity);
            return entity;
        });

        service = new TaskService(
                taskRepository,
                taskStepRepository,
                projectRepository,
                new StubWorkspaceContextService(),
                auditLogService,
                ledgerService,
                providerCatalogService,
                new StubWorkspaceOnboardingService()
        );
    }

    @Test
    @DisplayName("listTasks returns mapped task summary responses for workspace")
    void shouldListTasksForWorkspace() {
        TaskJpaEntity task = defaultTask();
        when(taskRepository.findByWorkspaceIdOrderByUpdatedAtDesc(eq(1L), any(Pageable.class)))
                .thenReturn(List.of(task));

        List<TaskSummaryResponse> result = service.listTasks(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo("task-abc12345");
        assertThat(result.get(0).taskType()).isEqualTo("research");
        assertThat(result.get(0).title()).isEqualTo("Analisar dados do mercado");
        assertThat(result.get(0).runtimeState()).isEqualTo("queued");
        assertThat(result.get(0).ownerName()).isEqualTo("Lume Operator");
    }

    @Test
    @DisplayName("createTask without runtime selection persists task and records audit+ledger")
    void shouldCreateTaskWithoutRuntimeSelection() {
        var request = new CreateTaskRequest("Analisar dados do mercado", "research", null);

        TaskDetailResponse result = service.createTask(request);

        assertThat(result.task().taskType()).isEqualTo("research");
        assertThat(result.task().title()).isEqualTo("Analisar dados do mercado");
        assertThat(result.task().statusLabel()).isEqualTo("Na fila");
        assertThat(result.task().runtimeState()).isEqualTo("queued");
        assertThat(result.task().providerCode()).isNull();
        assertThat(result.task().modelCode()).isNull();
        assertThat(result.task().ownerName()).isEqualTo("Lume Operator");
        assertThat(auditLogService.recordedActions).contains("created");
        verify(taskRepository).save(any());
    }

    @Test
    @DisplayName("createTask with valid provider and model persists runtime selection")
    void shouldCreateTaskWithValidRuntimeSelection() {
        var request = new CreateTaskRequest(
                "Gerar relatorio financeiro", "generation", null,
                "openai", "gpt-4", null
        );

        TaskDetailResponse result = service.createTask(request);

        assertThat(result.task().providerCode()).isEqualTo("openai");
        assertThat(result.task().modelCode()).isEqualTo("gpt-4");
        assertThat(result.task().versionLabel()).isEqualTo("v2024-01");
        assertThat(result.task().runtimeState()).isEqualTo("queued");
        assertThat(auditLogService.recordedActions).contains("created");
        verify(taskRepository).save(any());
    }

    @Test
    @DisplayName("createTask with provider but no model throws IllegalArgumentException")
    void shouldRejectTaskWithProviderButNoModel() {
        var request = new CreateTaskRequest(
                "Tarefa incompleta", "research", null,
                "openai", null, null
        );

        assertThatThrownBy(() -> service.createTask(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("provedor e modelo");
    }

    @Test
    @DisplayName("getTask returns detail response with steps")
    void shouldGetTaskDetailWithSteps() {
        TaskJpaEntity task = defaultTask();
        when(taskRepository.findByIdAndWorkspaceId("task-abc12345", 1L))
                .thenReturn(Optional.of(task));

        TaskStepJpaEntity step = new TaskStepJpaEntity();
        step.setId("step-1");
        step.setTaskId("task-abc12345");
        step.setStepOrder(1);
        step.setStepType("analysis");
        step.setTitle("Coleta de dados");
        step.setDetail("Coletar dados de mercado");
        step.setStatusLabel("Concluido");

        when(taskStepRepository.findByTaskIdOrderByStepOrderAsc("task-abc12345"))
                .thenReturn(List.of(step));

        TaskDetailResponse result = service.getTask("task-abc12345");

        assertThat(result.task().id()).isEqualTo("task-abc12345");
        assertThat(result.task().taskType()).isEqualTo("research");
        assertThat(result.steps()).hasSize(1);
        assertThat(result.steps().get(0).id()).isEqualTo("step-1");
        assertThat(result.steps().get(0).stepOrder()).isEqualTo(1);
        assertThat(result.steps().get(0).stepType()).isEqualTo("analysis");
        assertThat(result.steps().get(0).title()).isEqualTo("Coleta de dados");
        assertThat(result.steps().get(0).statusLabel()).isEqualTo("Concluido");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private TaskJpaEntity defaultTask() {
        TaskJpaEntity task = new TaskJpaEntity();
        task.setId("task-abc12345");
        task.setWorkspaceId(1L);
        task.setProjectId(null);
        task.setTaskType("research");
        task.setTitle("Analisar dados do mercado");
        task.setPrompt("Analisar dados do mercado");
        task.setSummary("");
        task.setStatusLabel("Na fila");
        task.setAvailability("live");
        task.setRuntimeState("queued");
        task.setLastError(null);
        task.setOwnerName("Lume Operator");
        task.setProviderCode(null);
        task.setModelCode(null);
        task.setVersionLabel(null);
        task.setScheduledFor(null);
        task.setShareSlug(null);
        triggerPrePersist(task);
        return task;
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
        public ProviderDefinition requireProvider(String providerCode) {
            return new ProviderDefinition(
                    "openai",
                    "OpenAI",
                    "text-runtime",
                    InferenceProtocol.OPENAI_CHAT_COMPLETIONS,
                    true,
                    "https://api.openai.com/v1",
                    List.of(new CredentialFieldDefinition("api_key", "API Key", "OPENAI_API_KEY", true, true, "OpenAI API key")),
                    "bearer",
                    "openai",
                    List.of(),
                    false,
                    false,
                    true,
                    false,
                    "active",
                    "https://platform.openai.com/api-keys",
                    "https://platform.openai.com/docs",
                    "gpt-4",
                    List.of("chat", "completion"),
                    null
            );
        }

        @Override
        public Optional<ModelDefinition> findModelForProvider(String providerCode, String modelCode) {
            if ("openai".equals(providerCode) && "gpt-4".equals(modelCode)) {
                return Optional.of(new ModelDefinition("gpt-4", "openai", "GPT-4", "v2024-01", true, true));
            }
            return Optional.empty();
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
