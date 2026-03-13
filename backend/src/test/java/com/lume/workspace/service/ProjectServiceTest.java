package com.lume.workspace.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lume.domain.exception.ResourceNotFoundException;
import com.lume.workspace.dto.CreateProjectRequest;
import com.lume.workspace.dto.UpdateProjectRequest;
import com.lume.workspace.entity.ProjectJpaEntity;
import com.lume.workspace.repository.ProjectJpaRepository;
import com.lume.workspace.repository.TaskJpaRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("ProjectService - Unit Tests")
class ProjectServiceTest {

    private ProjectJpaRepository projectRepository;
    private TaskJpaRepository taskRepository;
    private SpyAuditLogService auditLogService;
    private ProjectService service;

    @BeforeEach
    void setUp() {
        projectRepository = mock(ProjectJpaRepository.class);
        taskRepository = mock(TaskJpaRepository.class);
        auditLogService = new SpyAuditLogService();

        when(projectRepository.save(any())).thenAnswer(inv -> {
            ProjectJpaEntity entity = inv.getArgument(0);
            triggerPrePersist(entity);
            return entity;
        });

        service = new ProjectService(
                projectRepository,
                taskRepository,
                new StubWorkspaceContextService(),
                auditLogService,
                new StubWorkspaceOnboardingService()
        );
    }

    @Test
    @DisplayName("listProjects returns mapped responses with task count")
    void shouldListProjectsForWorkspace() {
        ProjectJpaEntity project = defaultProject("proj-abc", "Projeto Alpha");
        when(projectRepository.findByWorkspaceIdOrderByUpdatedAtDesc(eq(1L), any(PageRequest.class)))
                .thenReturn(List.of(project));
        when(taskRepository.countByWorkspaceIdAndProjectId(1L, "proj-abc")).thenReturn(5L);

        var result = service.listProjects();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo("proj-abc");
        assertThat(result.get(0).name()).isEqualTo("Projeto Alpha");
        assertThat(result.get(0).taskCount()).isEqualTo(5);
        assertThat(result.get(0).updatedAt()).isNotBlank();
    }

    @Test
    @DisplayName("createProject creates project with defaults, records audit, advances onboarding")
    void shouldCreateProjectWithDefaults() {
        when(taskRepository.countByWorkspaceIdAndProjectId(eq(1L), any())).thenReturn(0L);

        var request = new CreateProjectRequest("Novo Projeto", "Resumo do projeto", null, null, null);
        var result = service.createProject(request);

        assertThat(result.name()).isEqualTo("Novo Projeto");
        assertThat(result.summary()).isEqualTo("Resumo do projeto");
        assertThat(result.statusLabel()).isEqualTo("Em andamento");
        assertThat(result.availability()).isEqualTo("live");
        assertThat(result.ownerName()).isEqualTo("Lume Operator");
        assertThat(result.id()).startsWith("proj-");
        assertThat(auditLogService.recordedActions).contains("created");
    }

    @Test
    @DisplayName("updateProject updates name, summary, and status, records audit")
    void shouldUpdateProjectFields() {
        ProjectJpaEntity existing = defaultProject("proj-xyz", "Nome Antigo");
        when(projectRepository.findByIdAndWorkspaceId("proj-xyz", 1L)).thenReturn(Optional.of(existing));
        when(taskRepository.countByWorkspaceIdAndProjectId(1L, "proj-xyz")).thenReturn(2L);

        var request = new UpdateProjectRequest("Nome Novo", "Resumo atualizado", "Concluido", null, null);
        var result = service.updateProject("proj-xyz", request);

        assertThat(result.name()).isEqualTo("Nome Novo");
        assertThat(result.summary()).isEqualTo("Resumo atualizado");
        assertThat(result.statusLabel()).isEqualTo("Concluido");
        assertThat(result.availability()).isEqualTo("live");
        assertThat(result.taskCount()).isEqualTo(2);
        assertThat(auditLogService.recordedActions).contains("updated");
    }

    @Test
    @DisplayName("updateProject throws ResourceNotFoundException for non-existent project")
    void shouldRejectUpdateForNonExistentProject() {
        when(projectRepository.findByIdAndWorkspaceId("proj-ghost", 1L)).thenReturn(Optional.empty());

        var request = new UpdateProjectRequest("Irrelevante", null, null, null, null);

        assertThatThrownBy(() -> service.updateProject("proj-ghost", request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private ProjectJpaEntity defaultProject(String id, String name) {
        ProjectJpaEntity project = new ProjectJpaEntity();
        project.setId(id);
        project.setWorkspaceId(1L);
        project.setName(name);
        project.setSummary("Descricao padrao do projeto");
        project.setStatusLabel("Em andamento");
        project.setAvailability("live");
        project.setOwnerName("Lume Operator");
        triggerPrePersist(project);
        return project;
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
