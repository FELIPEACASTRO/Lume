package com.lume.workspace.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lume.workspace.dto.ArtifactVersionResponse;
import com.lume.workspace.dto.CreateArtifactVersionRequest;
import com.lume.workspace.dto.LibraryEntryResponse;
import com.lume.workspace.entity.ArtifactVersionJpaEntity;
import com.lume.workspace.entity.LibraryEntryJpaEntity;
import com.lume.workspace.entity.ProjectJpaEntity;
import com.lume.workspace.repository.ArtifactVersionJpaRepository;
import com.lume.workspace.repository.LibraryEntryJpaRepository;
import com.lume.workspace.repository.ProjectJpaRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("LibraryService - Unit Tests")
class LibraryServiceTest {

    @Test
    @DisplayName("should enrich library entries with version and project metadata")
    void shouldEnrichLibraryEntriesWithVersionAndProjectMetadata() {
        LibraryEntryJpaRepository libraryRepository = mock(LibraryEntryJpaRepository.class);
        ArtifactVersionJpaRepository versionRepository = mock(ArtifactVersionJpaRepository.class);
        ProjectJpaRepository projectRepository = mock(ProjectJpaRepository.class);

        LibraryEntryJpaEntity entry = new LibraryEntryJpaEntity();
        entry.setId("lib-onboarding");
        entry.setWorkspaceId(1L);
        entry.setTitle("Playbook");
        entry.setCategory("Playbook");
        entry.setEntryType("artifact");
        entry.setProjectId("proj-ops");
        entry.setStatusLabel("Ativo");
        entry.setAvailability("live");
        entry.setOwnerName("Operacao");
        entry.setSourceLabel("Backend do workspace");
        entry.setFavorited(true);
        entry.setArchived(false);
        entry.setSummary("Fluxo operacional.");
        entry.setTags(new LinkedHashSet<>(List.of("ops", "onboarding")));

        ArtifactVersionJpaEntity version = new ArtifactVersionJpaEntity();
        version.setId("ver-1");
        version.setEntryId("lib-onboarding");
        version.setWorkspaceId(1L);
        version.setVersionLabel("v2");
        version.setChangeSummary("Nova aprovacao");
        version.setContentPreview("Preview");
        version.setCreatedByName("Lume Operator");

        ProjectJpaEntity project = new ProjectJpaEntity();
        project.setId("proj-ops");
        project.setWorkspaceId(1L);
        project.setName("Operacao");

        when(libraryRepository.findByWorkspaceIdOrderByUpdatedAtDesc(1L)).thenReturn(List.of(entry));
        when(versionRepository.findByWorkspaceIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(version));
        when(projectRepository.findByWorkspaceIdOrderByUpdatedAtDesc(1L)).thenReturn(List.of(project));

        LibraryService service = new LibraryService(
                libraryRepository,
                versionRepository,
                projectRepository,
                new StubWorkspaceContextService(),
                new NoOpAuditLogService()
        );

        List<LibraryEntryResponse> response = service.findAll("play", null);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().projectName()).isEqualTo("Operacao");
        assertThat(response.getFirst().versionCount()).isEqualTo(1);
        assertThat(response.getFirst().currentVersionLabel()).isEqualTo("v2");
        assertThat(response.getFirst().favorited()).isTrue();
    }

    @Test
    @DisplayName("should create artifact version and persist audit state")
    void shouldCreateArtifactVersionAndPersistAuditState() {
        LibraryEntryJpaRepository libraryRepository = mock(LibraryEntryJpaRepository.class);
        ArtifactVersionJpaRepository versionRepository = mock(ArtifactVersionJpaRepository.class);
        ProjectJpaRepository projectRepository = mock(ProjectJpaRepository.class);

        LibraryEntryJpaEntity entry = new LibraryEntryJpaEntity();
        entry.setId("lib-onboarding");
        entry.setWorkspaceId(1L);
        entry.setTitle("Playbook");
        entry.setCategory("Playbook");
        entry.setEntryType("artifact");
        entry.setStatusLabel("Ativo");
        entry.setAvailability("live");
        entry.setOwnerName("Operacao");
        entry.setSourceLabel("Backend do workspace");
        entry.setSummary("Resumo");
        entry.setTags(new LinkedHashSet<>(List.of("ops")));

        when(libraryRepository.findByIdAndWorkspaceId("lib-onboarding", 1L)).thenReturn(Optional.of(entry));
        when(versionRepository.save(any(ArtifactVersionJpaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(libraryRepository.save(any(LibraryEntryJpaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LibraryService service = new LibraryService(
                libraryRepository,
                versionRepository,
                projectRepository,
                new StubWorkspaceContextService(),
                new NoOpAuditLogService()
        );

        ArtifactVersionResponse response = service.createVersion("lib-onboarding", new CreateArtifactVersionRequest(
                "v3",
                "Ajuste de ownership",
                "Novo preview do artefato."
        ));

        verify(versionRepository).save(any(ArtifactVersionJpaEntity.class));
        verify(libraryRepository).save(any(LibraryEntryJpaEntity.class));
        assertThat(response.versionLabel()).isEqualTo("v3");
        assertThat(response.changeSummary()).isEqualTo("Ajuste de ownership");
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
        public String getActorName() {
            return "Lume Operator";
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
