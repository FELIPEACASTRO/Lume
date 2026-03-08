package com.lume.workspace.service;

import com.lume.workspace.dto.ProjectResponse;
import com.lume.workspace.entity.ProjectJpaEntity;
import com.lume.workspace.repository.ProjectJpaRepository;
import com.lume.workspace.repository.TaskJpaRepository;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ProjectService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final ProjectJpaRepository projectRepository;
    private final TaskJpaRepository taskRepository;
    private final WorkspaceContextService workspaceContextService;

    public ProjectService(
            ProjectJpaRepository projectRepository,
            TaskJpaRepository taskRepository,
            WorkspaceContextService workspaceContextService
    ) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.workspaceContextService = workspaceContextService;
    }

    public List<ProjectResponse> listProjects() {
        return projectRepository.findByWorkspaceIdOrderByUpdatedAtDesc(workspaceContextService.getWorkspaceId()).stream()
                .map(this::toResponse)
                .toList();
    }

    private ProjectResponse toResponse(ProjectJpaEntity project) {
        int taskCount = taskRepository.findByWorkspaceIdAndProjectIdOrderByUpdatedAtDesc(
                workspaceContextService.getWorkspaceId(),
                project.getId()
        ).size();
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
}
