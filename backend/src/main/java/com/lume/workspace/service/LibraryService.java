package com.lume.workspace.service;

import com.lume.domain.exception.ResourceNotFoundException;
import com.lume.workspace.dto.ArtifactVersionResponse;
import com.lume.workspace.dto.CreateArtifactVersionRequest;
import com.lume.workspace.dto.LibraryEntryResponse;
import com.lume.workspace.entity.ArtifactVersionJpaEntity;
import com.lume.workspace.entity.LibraryEntryJpaEntity;
import com.lume.workspace.entity.ProjectJpaEntity;
import com.lume.workspace.repository.ArtifactVersionJpaRepository;
import com.lume.workspace.repository.LibraryEntryJpaRepository;
import com.lume.workspace.repository.ProjectJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LibraryService {

    private final LibraryEntryJpaRepository libraryEntryRepository;
    private final ArtifactVersionJpaRepository artifactVersionRepository;
    private final ProjectJpaRepository projectRepository;
    private final WorkspaceContextService workspaceContextService;
    private final AuditLogService auditLogService;

    public LibraryService(
            LibraryEntryJpaRepository libraryEntryRepository,
            ArtifactVersionJpaRepository artifactVersionRepository,
            ProjectJpaRepository projectRepository,
            WorkspaceContextService workspaceContextService,
            AuditLogService auditLogService
    ) {
        this.libraryEntryRepository = libraryEntryRepository;
        this.artifactVersionRepository = artifactVersionRepository;
        this.projectRepository = projectRepository;
        this.workspaceContextService = workspaceContextService;
        this.auditLogService = auditLogService;
    }

    public List<LibraryEntryResponse> findAll(String query, String category) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_ARTIFACTS_READ);
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase();
        String normalizedCategory = category == null ? "" : category.trim().toLowerCase();
        Long workspaceId = workspaceContextService.getWorkspaceId();
        Map<String, ProjectJpaEntity> projectsById = loadProjectsById(workspaceId);
        Map<String, List<ArtifactVersionJpaEntity>> versionsByEntryId = artifactVersionRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId)
                .stream()
                .collect(Collectors.groupingBy(ArtifactVersionJpaEntity::getEntryId));

        return libraryEntryRepository.findByWorkspaceIdOrderByUpdatedAtDesc(workspaceId, PageRequest.of(0, 200))
                .stream()
                .filter(entry -> !entry.isArchived())
                .filter(entry -> normalizedCategory.isBlank() || entry.getCategory().equalsIgnoreCase(normalizedCategory))
                .filter(entry -> normalizedQuery.isBlank() || matchesQuery(entry, normalizedQuery))
                .map(entry -> toResponse(entry, projectsById.get(entry.getProjectId()), versionsByEntryId.get(entry.getId())))
                .toList();
    }

    public LibraryEntryResponse findById(String id) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_ARTIFACTS_READ);
        Long workspaceId = workspaceContextService.getWorkspaceId();
        LibraryEntryJpaEntity entry = libraryEntryRepository.findByIdAndWorkspaceId(id, workspaceContextService.getWorkspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("LibraryEntry", id));
        List<ArtifactVersionJpaEntity> versions = artifactVersionRepository.findByEntryIdAndWorkspaceIdOrderByCreatedAtDesc(id, workspaceId);
        ProjectJpaEntity project = entry.getProjectId() == null
                ? null
                : projectRepository.findByIdAndWorkspaceId(entry.getProjectId(), workspaceId).orElse(null);
        return toResponse(entry, project, versions);
    }

    public List<ArtifactVersionResponse> listVersions(String entryId) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_ARTIFACTS_READ);
        findEntry(entryId);
        return artifactVersionRepository.findByEntryIdAndWorkspaceIdOrderByCreatedAtDesc(entryId, workspaceContextService.getWorkspaceId())
                .stream()
                .map(this::toVersionResponse)
                .toList();
    }

    @Transactional
    public ArtifactVersionResponse createVersion(String entryId, CreateArtifactVersionRequest request) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_ARTIFACTS_MANAGE);
        LibraryEntryJpaEntity entry = findEntry(entryId);

        ArtifactVersionJpaEntity version = new ArtifactVersionJpaEntity();
        version.setId(UUID.randomUUID().toString());
        version.setEntryId(entryId);
        version.setWorkspaceId(workspaceContextService.getWorkspaceId());
        version.setVersionLabel(request.versionLabel().trim());
        version.setChangeSummary(request.changeSummary().trim());
        version.setContentPreview(request.contentPreview().trim());
        version.setCreatedByName(workspaceContextService.getActorName());
        artifactVersionRepository.save(version);

        entry.setStatusLabel("Versionado");
        entry.setAvailability("live");
        libraryEntryRepository.save(entry);

        auditLogService.record(
                "artifact_version",
                version.getId(),
                "created",
                Map.of(
                        "entryId", entryId,
                        "versionLabel", version.getVersionLabel(),
                        "changeSummary", version.getChangeSummary()
                )
        );

        return toVersionResponse(version);
    }

    private boolean matchesQuery(LibraryEntryJpaEntity entry, String normalizedQuery) {
        String haystack = String.join(
                " ",
                entry.getTitle(),
                entry.getSummary(),
                entry.getOwnerName(),
                entry.getCategory(),
                entry.getEntryType(),
                entry.getProjectId() == null ? "" : entry.getProjectId(),
                String.join(" ", entry.getTags())
        ).toLowerCase();
        return haystack.contains(normalizedQuery);
    }

    private LibraryEntryJpaEntity findEntry(String entryId) {
        return libraryEntryRepository.findByIdAndWorkspaceId(entryId, workspaceContextService.getWorkspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("LibraryEntry", entryId));
    }

    private Map<String, ProjectJpaEntity> loadProjectsById(Long workspaceId) {
        return projectRepository.findByWorkspaceIdOrderByUpdatedAtDesc(workspaceId, PageRequest.of(0, 200))
                .stream()
                .collect(Collectors.toMap(ProjectJpaEntity::getId, Function.identity(), (left, right) -> left));
    }

    private LibraryEntryResponse toResponse(
            LibraryEntryJpaEntity entry,
            ProjectJpaEntity project,
            List<ArtifactVersionJpaEntity> versions
    ) {
        List<String> tags = entry.getTags().stream().sorted(Comparator.naturalOrder()).toList();
        List<ArtifactVersionJpaEntity> safeVersions = versions == null ? List.of() : versions;
        ArtifactVersionJpaEntity latestVersion = safeVersions.stream()
                .max(Comparator.comparing(ArtifactVersionJpaEntity::getCreatedAt))
                .orElse(null);

        return new LibraryEntryResponse(
                entry.getId(),
                entry.getTitle(),
                entry.getCategory(),
                entry.getEntryType(),
                entry.getStatusLabel(),
                entry.getAvailability(),
                entry.getOwnerName(),
                entry.getSourceLabel(),
                entry.getSummary(),
                tags,
                entry.getProjectId(),
                project != null ? project.getName() : null,
                entry.isFavorited(),
                entry.isArchived(),
                safeVersions.size(),
                latestVersion != null ? latestVersion.getVersionLabel() : null
        );
    }

    private ArtifactVersionResponse toVersionResponse(ArtifactVersionJpaEntity version) {
        return new ArtifactVersionResponse(
                version.getId(),
                version.getEntryId(),
                version.getVersionLabel(),
                version.getChangeSummary(),
                version.getContentPreview(),
                version.getCreatedByName(),
                format(version.getCreatedAt())
        );
    }

    private String format(LocalDateTime temporal) {
        return temporal == null ? null : temporal.toString().replace('T', ' ');
    }
}
