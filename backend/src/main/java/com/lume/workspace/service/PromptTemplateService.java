package com.lume.workspace.service;

import com.lume.domain.exception.ResourceNotFoundException;
import com.lume.workspace.dto.CreatePromptTemplateRequest;
import com.lume.workspace.dto.PromptTemplateResponse;
import com.lume.workspace.dto.UpdatePromptTemplateRequest;
import com.lume.workspace.entity.AgentProfileJpaEntity;
import com.lume.workspace.entity.ProjectJpaEntity;
import com.lume.workspace.entity.PromptTemplateJpaEntity;
import com.lume.workspace.repository.AgentProfileJpaRepository;
import com.lume.workspace.repository.ProjectJpaRepository;
import com.lume.workspace.repository.PromptTemplateJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PromptTemplateService {

    private final PromptTemplateJpaRepository promptTemplateRepository;
    private final ProjectJpaRepository projectRepository;
    private final AgentProfileJpaRepository agentProfileRepository;
    private final WorkspaceContextService workspaceContextService;
    private final AuditLogService auditLogService;

    public PromptTemplateService(
            PromptTemplateJpaRepository promptTemplateRepository,
            ProjectJpaRepository projectRepository,
            AgentProfileJpaRepository agentProfileRepository,
            WorkspaceContextService workspaceContextService,
            AuditLogService auditLogService
    ) {
        this.promptTemplateRepository = promptTemplateRepository;
        this.projectRepository = projectRepository;
        this.agentProfileRepository = agentProfileRepository;
        this.workspaceContextService = workspaceContextService;
        this.auditLogService = auditLogService;
    }

    public List<PromptTemplateResponse> findAll(String query, String projectId, String agentProfileId, Boolean favoritedOnly) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_TEMPLATES_READ);
        Long workspaceId = workspaceContextService.getWorkspaceId();
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase();
        Map<String, ProjectJpaEntity> projectsById = projectRepository.findByWorkspaceIdOrderByUpdatedAtDesc(workspaceId)
                .stream()
                .collect(Collectors.toMap(ProjectJpaEntity::getId, Function.identity(), (left, right) -> left));
        Map<String, AgentProfileJpaEntity> agentsById = agentProfileRepository.findByWorkspaceIdOrderByNameAsc(workspaceId)
                .stream()
                .collect(Collectors.toMap(AgentProfileJpaEntity::getId, Function.identity(), (left, right) -> left));

        return promptTemplateRepository.findByWorkspaceIdOrderByUpdatedAtDesc(workspaceId)
                .stream()
                .filter(template -> projectId == null || projectId.isBlank() || projectId.equals(template.getProjectId()))
                .filter(template -> agentProfileId == null || agentProfileId.isBlank() || agentProfileId.equals(template.getAgentProfileId()))
                .filter(template -> favoritedOnly == null || !favoritedOnly || template.isFavorited())
                .filter(template -> normalizedQuery.isBlank() || matchesQuery(template, normalizedQuery))
                .map(template -> toResponse(template, projectsById.get(template.getProjectId()), agentsById.get(template.getAgentProfileId())))
                .toList();
    }

    @Transactional
    public PromptTemplateResponse create(CreatePromptTemplateRequest request) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_TEMPLATES_MANAGE);
        ProjectJpaEntity project = resolveProject(request.projectId());
        AgentProfileJpaEntity agentProfile = resolveAgentProfile(request.agentProfileId());

        PromptTemplateJpaEntity template = new PromptTemplateJpaEntity();
        template.setId(UUID.randomUUID().toString());
        template.setWorkspaceId(workspaceContextService.getWorkspaceId());
        template.setProjectId(project != null ? project.getId() : null);
        template.setAgentProfileId(agentProfile != null ? agentProfile.getId() : null);
        template.setTitle(request.title().trim());
        template.setSummary(request.summary().trim());
        template.setPromptBody(request.promptBody().trim());
        template.setTemplateScope(resolveTemplateScope(request.templateScope()));
        template.setStatusLabel(resolveStatusLabel(request.statusLabel()));
        template.setAvailability(resolveAvailability(request.availability()));
        template.setOwnerName(workspaceContextService.getActorName());
        template.setVariablesRaw(joinVariables(request.variables()));
        template.setFavorited(Boolean.TRUE.equals(request.favorited()));
        promptTemplateRepository.save(template);

        auditLogService.record(
                "prompt_template",
                template.getId(),
                "created",
                Map.of(
                        "title", template.getTitle(),
                        "projectId", template.getProjectId() == null ? "" : template.getProjectId(),
                        "agentProfileId", template.getAgentProfileId() == null ? "" : template.getAgentProfileId()
                )
        );

        return toResponse(template, project, agentProfile);
    }

    @Transactional
    public PromptTemplateResponse update(String id, UpdatePromptTemplateRequest request) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_TEMPLATES_MANAGE);
        PromptTemplateJpaEntity template = findTemplate(id);
        ProjectJpaEntity project = request.projectId() != null ? resolveProject(request.projectId()) : resolveProject(template.getProjectId());
        AgentProfileJpaEntity agentProfile = request.agentProfileId() != null ? resolveAgentProfile(request.agentProfileId()) : resolveAgentProfile(template.getAgentProfileId());

        if (request.title() != null && !request.title().isBlank()) {
            template.setTitle(request.title().trim());
        }
        if (request.summary() != null && !request.summary().isBlank()) {
            template.setSummary(request.summary().trim());
        }
        if (request.promptBody() != null && !request.promptBody().isBlank()) {
            template.setPromptBody(request.promptBody().trim());
        }
        if (request.templateScope() != null) {
            template.setTemplateScope(resolveTemplateScope(request.templateScope()));
        }
        if (request.projectId() != null) {
            template.setProjectId(project != null ? project.getId() : null);
        }
        if (request.agentProfileId() != null) {
            template.setAgentProfileId(agentProfile != null ? agentProfile.getId() : null);
        }
        if (request.variables() != null) {
            template.setVariablesRaw(joinVariables(request.variables()));
        }
        if (request.favorited() != null) {
            template.setFavorited(request.favorited());
        }
        if (request.statusLabel() != null && !request.statusLabel().isBlank()) {
            template.setStatusLabel(request.statusLabel().trim());
        }
        if (request.availability() != null && !request.availability().isBlank()) {
            template.setAvailability(request.availability().trim());
        }

        promptTemplateRepository.save(template);

        auditLogService.record(
                "prompt_template",
                template.getId(),
                "updated",
                Map.of(
                        "favorited", template.isFavorited(),
                        "templateScope", template.getTemplateScope()
                )
        );

        return toResponse(template, project, agentProfile);
    }

    @Transactional
    public void delete(String id) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_TEMPLATES_MANAGE);
        PromptTemplateJpaEntity template = findTemplate(id);
        promptTemplateRepository.delete(template);
        auditLogService.record("prompt_template", id, "deleted", Map.of("title", template.getTitle()));
    }

    @Transactional
    public PromptTemplateResponse markUsed(String id) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_TEMPLATES_READ);
        PromptTemplateJpaEntity template = findTemplate(id);
        template.setLastUsedAt(LocalDateTime.now());
        promptTemplateRepository.save(template);
        ProjectJpaEntity project = resolveProject(template.getProjectId());
        AgentProfileJpaEntity agentProfile = resolveAgentProfile(template.getAgentProfileId());
        return toResponse(template, project, agentProfile);
    }

    private PromptTemplateJpaEntity findTemplate(String id) {
        return promptTemplateRepository.findByIdAndWorkspaceId(id, workspaceContextService.getWorkspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("PromptTemplate", id));
    }

    private ProjectJpaEntity resolveProject(String projectId) {
        if (projectId == null || projectId.isBlank()) {
            return null;
        }
        return projectRepository.findByIdAndWorkspaceId(projectId, workspaceContextService.getWorkspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Projeto", projectId));
    }

    private AgentProfileJpaEntity resolveAgentProfile(String agentProfileId) {
        if (agentProfileId == null || agentProfileId.isBlank()) {
            return null;
        }
        return agentProfileRepository.findByIdAndWorkspaceId(agentProfileId, workspaceContextService.getWorkspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("AgentProfile", agentProfileId));
    }

    private boolean matchesQuery(PromptTemplateJpaEntity template, String normalizedQuery) {
        String haystack = String.join(
                " ",
                template.getTitle(),
                template.getSummary(),
                template.getPromptBody(),
                template.getTemplateScope(),
                template.getOwnerName(),
                template.getVariablesRaw()
        ).toLowerCase();
        return haystack.contains(normalizedQuery);
    }

    private String resolveTemplateScope(String templateScope) {
        if (templateScope == null || templateScope.isBlank()) {
            return "workspace";
        }
        return templateScope.trim().toLowerCase();
    }

    private String resolveStatusLabel(String statusLabel) {
        if (statusLabel == null || statusLabel.isBlank()) {
            return "Template operacional";
        }
        return statusLabel.trim();
    }

    private String resolveAvailability(String availability) {
        if (availability == null || availability.isBlank()) {
            return "live";
        }
        return availability.trim();
    }

    private String joinVariables(List<String> variables) {
        if (variables == null || variables.isEmpty()) {
            return "";
        }
        return variables.stream()
                .map(value -> value == null ? "" : value.trim())
                .filter(value -> !value.isBlank())
                .distinct()
                .collect(Collectors.joining(","));
    }

    private List<String> splitVariables(String variablesRaw) {
        if (variablesRaw == null || variablesRaw.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(variablesRaw.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private PromptTemplateResponse toResponse(
            PromptTemplateJpaEntity template,
            ProjectJpaEntity project,
            AgentProfileJpaEntity agentProfile
    ) {
        return new PromptTemplateResponse(
                template.getId(),
                template.getTitle(),
                template.getSummary(),
                template.getPromptBody(),
                splitVariables(template.getVariablesRaw()),
                template.getTemplateScope(),
                template.getStatusLabel(),
                template.getAvailability(),
                template.getOwnerName(),
                template.getProjectId(),
                project != null ? project.getName() : null,
                template.getAgentProfileId(),
                agentProfile != null ? agentProfile.getName() : null,
                template.isFavorited(),
                format(template.getLastUsedAt()),
                format(template.getUpdatedAt())
        );
    }

    private String format(LocalDateTime temporal) {
        return temporal == null ? null : temporal.toString().replace('T', ' ');
    }
}
