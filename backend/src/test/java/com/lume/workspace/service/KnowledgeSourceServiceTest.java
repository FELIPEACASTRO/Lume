package com.lume.workspace.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lume.domain.exception.ResourceNotFoundException;
import com.lume.workspace.dto.CreateKnowledgeSourceRequest;
import com.lume.workspace.dto.KnowledgeSourceResponse;
import com.lume.workspace.dto.UpdateKnowledgeSourceRequest;
import com.lume.workspace.entity.KnowledgeSourceJpaEntity;
import com.lume.workspace.entity.ProjectJpaEntity;
import com.lume.workspace.repository.KnowledgeSourceJpaRepository;
import com.lume.workspace.repository.ProjectJpaRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("KnowledgeSourceService - Unit Tests")
class KnowledgeSourceServiceTest {

    @Test
    @DisplayName("should list workspace knowledge sources with project labels")
    void shouldListWorkspaceKnowledgeSourcesWithProjectLabels() {
        KnowledgeSourceJpaRepository repository = mock(KnowledgeSourceJpaRepository.class);
        ProjectJpaRepository projectRepository = mock(ProjectJpaRepository.class);

        KnowledgeSourceJpaEntity entity = seededSource("knowledge-playbooks", "Playbooks", "proj-ops");
        when(repository.findByWorkspaceIdOrderByUpdatedAtDesc(1L)).thenReturn(List.of(entity));
        when(projectRepository.findByWorkspaceIdOrderByUpdatedAtDesc(1L)).thenReturn(List.of(project("proj-ops", "Operacao")));

        KnowledgeSourceService service = new KnowledgeSourceService(
                repository,
                projectRepository,
                new StubWorkspaceContextService(),
                new NoOpAuditLogService()
        );

        List<KnowledgeSourceResponse> response = service.listSources("play", null, null);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().projectName()).isEqualTo("Operacao");
        assertThat(response.getFirst().enabledForAgents()).isTrue();
    }

    @Test
    @DisplayName("should create knowledge source bound to current workspace")
    void shouldCreateKnowledgeSourceBoundToCurrentWorkspace() {
        KnowledgeSourceJpaRepository repository = mock(KnowledgeSourceJpaRepository.class);
        ProjectJpaRepository projectRepository = mock(ProjectJpaRepository.class);
        when(projectRepository.findByIdAndWorkspaceId("proj-ops", 1L)).thenReturn(Optional.of(project("proj-ops", "Operacao")));
        when(projectRepository.findByWorkspaceIdOrderByUpdatedAtDesc(1L)).thenReturn(List.of(project("proj-ops", "Operacao")));
        when(repository.save(any(KnowledgeSourceJpaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        KnowledgeSourceService service = new KnowledgeSourceService(
                repository,
                projectRepository,
                new StubWorkspaceContextService(),
                new NoOpAuditLogService()
        );

        KnowledgeSourceResponse response = service.createSource(new CreateKnowledgeSourceRequest(
                "Repositorio",
                "repository",
                "proj-ops",
                "https://example.com/repo",
                5,
                true,
                "Indexado",
                "live",
                "Fonte de conhecimento do time."
        ));

        verify(repository).save(any(KnowledgeSourceJpaEntity.class));
        assertThat(response.projectId()).isEqualTo("proj-ops");
        assertThat(response.documentCount()).isEqualTo(5);
        assertThat(response.sourceUri()).isEqualTo("https://example.com/repo");
    }

    @Test
    @DisplayName("should reject source creation when project is outside workspace")
    void shouldRejectSourceCreationWhenProjectIsOutsideWorkspace() {
        KnowledgeSourceJpaRepository repository = mock(KnowledgeSourceJpaRepository.class);
        ProjectJpaRepository projectRepository = mock(ProjectJpaRepository.class);
        when(projectRepository.findByIdAndWorkspaceId("proj-missing", 1L)).thenReturn(Optional.empty());

        KnowledgeSourceService service = new KnowledgeSourceService(
                repository,
                projectRepository,
                new StubWorkspaceContextService(),
                new NoOpAuditLogService()
        );

        assertThatThrownBy(() -> service.createSource(new CreateKnowledgeSourceRequest(
                "Repositorio",
                "repository",
                "proj-missing",
                "https://example.com/repo",
                5,
                true,
                "Indexado",
                "live",
                "Fonte de conhecimento do time."
        )))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Projeto");
    }

    @Test
    @DisplayName("should update source and refresh indexing timestamp when structural fields change")
    void shouldUpdateSourceAndRefreshIndexingTimestampWhenStructuralFieldsChange() {
        KnowledgeSourceJpaRepository repository = mock(KnowledgeSourceJpaRepository.class);
        ProjectJpaRepository projectRepository = mock(ProjectJpaRepository.class);
        KnowledgeSourceJpaEntity entity = seededSource("knowledge-playbooks", "Playbooks", "proj-ops");
        LocalDateTime previousIndex = entity.getLastIndexedAt();

        when(repository.findByIdAndWorkspaceId("knowledge-playbooks", 1L)).thenReturn(Optional.of(entity));
        when(repository.save(any(KnowledgeSourceJpaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(projectRepository.findByIdAndWorkspaceId("proj-ops", 1L)).thenReturn(Optional.of(project("proj-ops", "Operacao")));
        when(projectRepository.findByWorkspaceIdOrderByUpdatedAtDesc(1L)).thenReturn(List.of(project("proj-ops", "Operacao")));

        KnowledgeSourceService service = new KnowledgeSourceService(
                repository,
                projectRepository,
                new StubWorkspaceContextService(),
                new NoOpAuditLogService()
        );

        KnowledgeSourceResponse response = service.updateSource("knowledge-playbooks", new UpdateKnowledgeSourceRequest(
                null,
                null,
                "proj-ops",
                "https://example.com/updated",
                7,
                false,
                "Reindexado",
                null,
                "Atualizada."
        ));

        assertThat(response.enabledForAgents()).isFalse();
        assertThat(response.documentCount()).isEqualTo(7);
        assertThat(entity.getLastIndexedAt()).isAfterOrEqualTo(previousIndex);
    }

    private static KnowledgeSourceJpaEntity seededSource(String id, String title, String projectId) {
        KnowledgeSourceJpaEntity entity = new KnowledgeSourceJpaEntity();
        entity.setId(id);
        entity.setWorkspaceId(1L);
        entity.setTitle(title);
        entity.setSourceType("library");
        entity.setProjectId(projectId);
        entity.setStatusLabel("API real");
        entity.setAvailability("live");
        entity.setDocumentCount(3);
        entity.setEnabledForAgents(true);
        entity.setSourceUri("lume://library/playbooks");
        entity.setNote("Base operacional.");
        entity.setLastIndexedAt(LocalDateTime.now().minusHours(1));
        return entity;
    }

    private static ProjectJpaEntity project(String id, String name) {
        ProjectJpaEntity entity = new ProjectJpaEntity();
        entity.setId(id);
        entity.setName(name);
        entity.setWorkspaceId(1L);
        return entity;
    }

    private static final class StubWorkspaceContextService extends WorkspaceContextService {
        private StubWorkspaceContextService() {
            super(null, null, null, null, null, null, new ObjectProvider<>() {
                @Override
                public HttpServletRequest getObject(Object... args) {
                    return null;
                }

                @Override
                public HttpServletRequest getIfAvailable() {
                    return null;
                }

                @Override
                public HttpServletRequest getIfUnique() {
                    return null;
                }

                @Override
                public HttpServletRequest getObject() {
                    return null;
                }
            });
        }

        @Override
        public void requirePermission(String permission) {
        }

        @Override
        public Long getWorkspaceId() {
            return 1L;
        }

        @Override
        public Long getOrganizationId() {
            return 1L;
        }

        @Override
        public Long getActorUserIdOrNull() {
            return 1L;
        }
    }

    private static final class NoOpAuditLogService extends AuditLogService {
        private NoOpAuditLogService() {
            super(null, null, new ObjectMapper());
        }

        @Override
        public void record(String entityType, String entityId, String action, Object payload) {
        }
    }
}
