package com.lume.workspace.service;

import com.lume.domain.exception.ResourceNotFoundException;
import com.lume.workspace.dto.CreateTaskRequest;
import com.lume.workspace.dto.TaskDetailResponse;
import com.lume.workspace.dto.TaskStepResponse;
import com.lume.workspace.dto.TaskSummaryResponse;
import com.lume.workspace.entity.TaskJpaEntity;
import com.lume.workspace.repository.ProjectJpaRepository;
import com.lume.workspace.repository.TaskJpaRepository;
import com.lume.workspace.repository.TaskStepJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class TaskService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final TaskJpaRepository taskRepository;
    private final TaskStepJpaRepository taskStepRepository;
    private final ProjectJpaRepository projectRepository;
    private final WorkspaceContextService workspaceContextService;
    private final AuditLogService auditLogService;

    public TaskService(
            TaskJpaRepository taskRepository,
            TaskStepJpaRepository taskStepRepository,
            ProjectJpaRepository projectRepository,
            WorkspaceContextService workspaceContextService,
            AuditLogService auditLogService
    ) {
        this.taskRepository = taskRepository;
        this.taskStepRepository = taskStepRepository;
        this.projectRepository = projectRepository;
        this.workspaceContextService = workspaceContextService;
        this.auditLogService = auditLogService;
    }

    public List<TaskSummaryResponse> listTasks(String projectId) {
        List<TaskJpaEntity> tasks = projectId == null || projectId.isBlank()
                ? taskRepository.findByWorkspaceIdOrderByUpdatedAtDesc(workspaceContextService.getWorkspaceId())
                : taskRepository.findByWorkspaceIdAndProjectIdOrderByUpdatedAtDesc(workspaceContextService.getWorkspaceId(), projectId);
        return tasks.stream().map(this::toSummaryResponse).toList();
    }

    public TaskDetailResponse getTask(String taskId) {
        TaskJpaEntity task = taskRepository.findByIdAndWorkspaceId(taskId, workspaceContextService.getWorkspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Tarefa", taskId));
        return toDetailResponse(task);
    }

    @Transactional
    public TaskDetailResponse createTask(CreateTaskRequest request) {
        TaskJpaEntity task = new TaskJpaEntity();
        task.setId("task-" + UUID.randomUUID().toString().substring(0, 8));
        task.setWorkspaceId(workspaceContextService.getWorkspaceId());
        task.setProjectId(normalizeProjectId(request.projectId()));
        task.setTaskType(request.taskType());
        task.setTitle(titleFromPrompt(request.prompt(), request.taskType()));
        task.setPrompt(request.prompt());
        task.setSummary(summaryFromPrompt(request.prompt()));
        task.setStatusLabel("Na fila");
        task.setAvailability("live");
        task.setRuntimeState("queued");
        task.setLastError(null);
        task.setOwnerName(resolveOwnerName(task.getProjectId()));
        task.setScheduledFor(null);
        task.setShareSlug(null);
        TaskJpaEntity savedTask = taskRepository.save(task);

        auditLogService.record(
                "task",
                savedTask.getId(),
                "created",
                "{\"taskType\":\"%s\"}".formatted(savedTask.getTaskType())
        );

        return toDetailResponse(savedTask);
    }

    private TaskSummaryResponse toSummaryResponse(TaskJpaEntity task) {
        String projectName = task.getProjectId() == null
                ? null
                : projectRepository.findById(task.getProjectId()).map(project -> project.getName()).orElse(null);
        return new TaskSummaryResponse(
                task.getId(),
                task.getProjectId(),
                projectName,
                task.getTaskType(),
                task.getTitle(),
                task.getPrompt(),
                task.getSummary(),
                task.getStatusLabel(),
                task.getAvailability(),
                task.getRuntimeState(),
                task.getLastError(),
                task.getOwnerName(),
                task.getUpdatedAt().format(DATE_TIME_FORMATTER),
                task.getScheduledFor() == null ? null : task.getScheduledFor().format(DATE_TIME_FORMATTER),
                task.getShareSlug()
        );
    }

    private TaskDetailResponse toDetailResponse(TaskJpaEntity task) {
        List<TaskStepResponse> steps = taskStepRepository.findByTaskIdOrderByStepOrderAsc(task.getId()).stream()
                .map(step -> new TaskStepResponse(
                        step.getId(),
                        step.getStepOrder(),
                        step.getStepType(),
                        step.getTitle(),
                        step.getDetail(),
                        step.getStatusLabel()
                ))
                .toList();

        return new TaskDetailResponse(
                toSummaryResponse(task),
                steps,
                List.of()
        );
    }

    private String normalizeProjectId(String projectId) {
        if (projectId == null || projectId.isBlank()) {
            return null;
        }
        projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto", projectId));
        return projectId;
    }

    private String titleFromPrompt(String prompt, String taskType) {
        String normalized = prompt.trim().replaceAll("\\s+", " ");
        if (normalized.length() <= 72) {
            return normalized;
        }
        return "%s...".formatted(normalized.substring(0, 69));
    }

    private String summaryFromPrompt(String prompt) {
        String normalized = prompt.trim().replaceAll("\\s+", " ");
        if (normalized.length() <= 140) {
            return normalized;
        }
        return "%s...".formatted(normalized.substring(0, 137));
    }

    private String resolveOwnerName(String projectId) {
        if (projectId == null || projectId.isBlank()) {
            return workspaceContextService.getActorName();
        }
        return projectRepository.findByIdAndWorkspaceId(projectId, workspaceContextService.getWorkspaceId())
                .map(project -> project.getOwnerName() == null || project.getOwnerName().isBlank()
                        ? workspaceContextService.getActorName()
                        : project.getOwnerName())
                .orElse(workspaceContextService.getActorName());
    }
}
