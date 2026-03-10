package com.lume.workspace.service;

import com.lume.domain.exception.ResourceNotFoundException;
import com.lume.workspace.dto.CreateTaskRequest;
import com.lume.workspace.dto.TaskDetailResponse;
import com.lume.workspace.dto.TaskStepResponse;
import com.lume.workspace.dto.TaskSummaryResponse;
import com.lume.workspace.entity.TaskJpaEntity;
import com.lume.workspace.inference.ModelDefinition;
import com.lume.workspace.inference.ProviderDefinition;
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
    private final WorkspaceLedgerService workspaceLedgerService;
    private final ProviderCatalogService providerCatalogService;

    public TaskService(
            TaskJpaRepository taskRepository,
            TaskStepJpaRepository taskStepRepository,
            ProjectJpaRepository projectRepository,
            WorkspaceContextService workspaceContextService,
            AuditLogService auditLogService,
            WorkspaceLedgerService workspaceLedgerService,
            ProviderCatalogService providerCatalogService
    ) {
        this.taskRepository = taskRepository;
        this.taskStepRepository = taskStepRepository;
        this.projectRepository = projectRepository;
        this.workspaceContextService = workspaceContextService;
        this.auditLogService = auditLogService;
        this.workspaceLedgerService = workspaceLedgerService;
        this.providerCatalogService = providerCatalogService;
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
        RuntimeSelection runtimeSelection = resolveRuntimeSelection(request);

        TaskJpaEntity task = new TaskJpaEntity();
        task.setId("task-" + UUID.randomUUID().toString().substring(0, 8));
        task.setWorkspaceId(workspaceContextService.getWorkspaceId());
        task.setProjectId(normalizeProjectId(request.projectId()));
        task.setTaskType(request.taskType());
        task.setTitle(titleFromPrompt(request.prompt(), request.taskType()));
        task.setPrompt(request.prompt());
        task.setSummary("");
        task.setStatusLabel("Na fila");
        task.setAvailability("live");
        task.setRuntimeState("queued");
        task.setLastError(null);
        task.setProviderCode(runtimeSelection.providerCode());
        task.setModelCode(runtimeSelection.modelCode());
        task.setVersionLabel(runtimeSelection.versionLabel());
        task.setOwnerName(resolveOwnerName(task.getProjectId()));
        task.setScheduledFor(null);
        task.setShareSlug(null);
        TaskJpaEntity savedTask = taskRepository.save(task);

        auditLogService.record(
                "task",
                savedTask.getId(),
                "created",
                "{\"taskType\":\"%s\",\"providerCode\":\"%s\",\"modelCode\":\"%s\"}".formatted(
                        savedTask.getTaskType(),
                        savedTask.getProviderCode() == null ? "" : savedTask.getProviderCode(),
                        savedTask.getModelCode() == null ? "" : savedTask.getModelCode()
                )
        );
        workspaceLedgerService.recordUsageEvent(
                "task.created",
                "task",
                savedTask.getId(),
                "Nova tarefa criada no workspace com runtimeState queued."
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
                task.getShareSlug(),
                task.getProviderCode(),
                task.getModelCode(),
                task.getVersionLabel()
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

    private RuntimeSelection resolveRuntimeSelection(CreateTaskRequest request) {
        String providerCode = trimToNull(request.providerCode());
        String modelCode = trimToNull(request.modelCode());
        String versionLabel = trimToNull(request.versionLabel());

        if (providerCode == null && modelCode == null && versionLabel == null) {
            return RuntimeSelection.empty();
        }

        if (providerCode == null || modelCode == null) {
            throw new IllegalArgumentException("Informe provedor e modelo para registrar a execucao IA da tarefa.");
        }

        ProviderDefinition provider = providerCatalogService.requireProvider(providerCode);
        if (!provider.executionSupported() || !"text-runtime".equalsIgnoreCase(provider.category())) {
            throw new IllegalArgumentException("Selecione um provedor de texto ativo para criar tarefas com execucao IA.");
        }

        ModelDefinition model = providerCatalogService.findModelForProvider(provider.code(), modelCode)
                .orElseThrow(() -> new IllegalArgumentException("O modelo selecionado nao pertence ao provedor informado."));

        if (!model.enabledForAgents()) {
            throw new IllegalArgumentException("O modelo selecionado nao esta habilitado para tarefas/conversa.");
        }

        if (versionLabel != null && !versionLabel.equals(model.versionLabel())) {
            throw new IllegalArgumentException("A versao da tarefa deve corresponder a versao oficial do modelo selecionado.");
        }

        return new RuntimeSelection(provider.code(), model.code(), model.versionLabel());
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record RuntimeSelection(
            String providerCode,
            String modelCode,
            String versionLabel
    ) {
        private static RuntimeSelection empty() {
            return new RuntimeSelection(null, null, null);
        }
    }
}
