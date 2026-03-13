package com.lume.workspace.service;

import com.lume.domain.exception.ResourceNotFoundException;
import com.lume.workspace.dto.CreateProjectRequest;
import com.lume.workspace.dto.ProjectResponse;
import com.lume.workspace.dto.UpdateProjectRequest;
import com.lume.workspace.entity.ProjectJpaEntity;
import com.lume.workspace.repository.ProjectJpaRepository;
import com.lume.workspace.repository.TaskJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ProjectService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final ProjectJpaRepository projectRepository;
    private final TaskJpaRepository taskRepository;
    private final WorkspaceContextService workspaceContextService;
    private final AuditLogService auditLogService;
    private final WorkspaceOnboardingService workspaceOnboardingService;

    public ProjectService(
            ProjectJpaRepository projectRepository,
            TaskJpaRepository taskRepository,
            WorkspaceContextService workspaceContextService,
            AuditLogService auditLogService,
            WorkspaceOnboardingService workspaceOnboardingService
    ) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.workspaceContextService = workspaceContextService;
        this.auditLogService = auditLogService;
        this.workspaceOnboardingService = workspaceOnboardingService;
    }

    public List<ProjectResponse> listProjects() {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_WORKSPACE_READ);
        return projectRepository.findByWorkspaceIdOrderByUpdatedAtDesc(
                workspaceContextService.getWorkspaceId(), PageRequest.of(0, 200)).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_WORKSPACE_READ);

        ProjectJpaEntity project = new ProjectJpaEntity();
        project.setId("proj-" + UUID.randomUUID().toString().substring(0, 8));
        project.setWorkspaceId(workspaceContextService.getWorkspaceId());
        project.setName(request.name().trim());
        project.setSummary(request.summary().trim());
        project.setStatusLabel(resolveStatusLabel(request.statusLabel()));
        project.setAvailability(resolveAvailability(request.availability()));
        project.setOwnerName(resolveOwnerName(request.ownerName()));
        ProjectJpaEntity saved = projectRepository.save(project);

        auditLogService.record(
                "project",
                saved.getId(),
                "created",
                Map.of(
                        "name", saved.getName(),
                        "workspaceId", String.valueOf(saved.getWorkspaceId()),
                        "ownerName", saved.getOwnerName()
                )
        );

        workspaceOnboardingService.advanceCurrentOnboarding(
                "first_project_created",
                "Primeiro projeto criado no workspace.",
                "project.created"
        );
        return toResponse(saved);
    }

    @Transactional
    public ProjectResponse updateProject(String id, UpdateProjectRequest request) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_WORKSPACE_READ);
        ProjectJpaEntity project = projectRepository.findByIdAndWorkspaceId(id, workspaceContextService.getWorkspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Projeto", id));

        if (request.name() != null && !request.name().isBlank()) {
            project.setName(request.name().trim());
        }
        if (request.summary() != null && !request.summary().isBlank()) {
            project.setSummary(request.summary().trim());
        }
        if (request.statusLabel() != null && !request.statusLabel().isBlank()) {
            project.setStatusLabel(resolveStatusLabel(request.statusLabel()));
        }
        if (request.availability() != null && !request.availability().isBlank()) {
            project.setAvailability(resolveAvailability(request.availability()));
        }
        if (request.ownerName() != null && !request.ownerName().isBlank()) {
            project.setOwnerName(resolveOwnerName(request.ownerName()));
        }

        ProjectJpaEntity saved = projectRepository.save(project);
        auditLogService.record(
                "project",
                saved.getId(),
                "updated",
                Map.of(
                        "name", saved.getName(),
                        "statusLabel", saved.getStatusLabel(),
                        "availability", saved.getAvailability()
                )
        );
        return toResponse(saved);
    }

    private ProjectResponse toResponse(ProjectJpaEntity project) {
        int taskCount = (int) taskRepository.countByWorkspaceIdAndProjectId(
                workspaceContextService.getWorkspaceId(),
                project.getId()
        );
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getSummary(),
                project.getStatusLabel(),
                project.getAvailability(),
                project.getOwnerName(),
                taskCount,
                project.getUpdatedAt().format(DATE_TIME_FORMATTER)
        );
    }

    private String resolveStatusLabel(String value) {
        if (value == null || value.isBlank()) {
            return "Em andamento";
        }
        return value.trim();
    }

    private String resolveAvailability(String value) {
        if (value == null || value.isBlank()) {
            return "live";
        }
        return value.trim().toLowerCase();
    }

    private String resolveOwnerName(String value) {
        if (value == null || value.isBlank()) {
            return workspaceContextService.getActorName();
        }
        return value.trim();
    }
}
