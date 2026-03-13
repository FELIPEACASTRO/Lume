package com.lume.workspace.service;

import com.lume.domain.exception.ResourceNotFoundException;
import com.lume.workspace.dto.CreateKnowledgeSourceRequest;
import com.lume.workspace.dto.KnowledgeSourceResponse;
import com.lume.workspace.dto.UpdateKnowledgeSourceRequest;
import com.lume.workspace.entity.KnowledgeSourceJpaEntity;
import com.lume.workspace.entity.ProjectJpaEntity;
import com.lume.workspace.repository.KnowledgeSourceJpaRepository;
import com.lume.workspace.repository.ProjectJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class KnowledgeSourceService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final KnowledgeSourceJpaRepository knowledgeSourceRepository;
    private final ProjectJpaRepository projectRepository;
    private final WorkspaceContextService workspaceContextService;
    private final AuditLogService auditLogService;

    public KnowledgeSourceService(
            KnowledgeSourceJpaRepository knowledgeSourceRepository,
            ProjectJpaRepository projectRepository,
            WorkspaceContextService workspaceContextService,
            AuditLogService auditLogService
    ) {
        this.knowledgeSourceRepository = knowledgeSourceRepository;
        this.projectRepository = projectRepository;
        this.workspaceContextService = workspaceContextService;
        this.auditLogService = auditLogService;
    }

    public int countSources() {
        return (int) knowledgeSourceRepository.countByWorkspaceId(workspaceContextService.getWorkspaceId());
    }

    public List<KnowledgeSourceResponse> listSources(String query, String projectId, Boolean enabledForAgents) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_KNOWLEDGE_READ);

        Long workspaceId = workspaceContextService.getWorkspaceId();
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase();
        String normalizedProjectId = normalizeOptional(projectId);

        List<KnowledgeSourceJpaEntity> sources = normalizedProjectId == null
                ? knowledgeSourceRepository.findByWorkspaceIdOrderByUpdatedAtDesc(workspaceId, PageRequest.of(0, 200))
                : knowledgeSourceRepository.findByWorkspaceIdAndProjectIdOrderByUpdatedAtDesc(workspaceId, normalizedProjectId);

        Map<String, String> projectNames = projectNames(workspaceId);

        return sources.stream()
                .filter(source -> enabledForAgents == null || enabledForAgents.equals(source.getEnabledForAgents()))
                .filter(source -> normalizedQuery.isBlank() || matchesQuery(source, normalizedQuery, projectNames))
                .map(source -> toResponse(source, projectNames))
                .toList();
    }

    public KnowledgeSourceResponse findById(String id) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_KNOWLEDGE_READ);
        Long workspaceId = workspaceContextService.getWorkspaceId();
        return toResponse(findSource(id, workspaceId), projectNames(workspaceId));
    }

    @Transactional
    public KnowledgeSourceResponse createSource(CreateKnowledgeSourceRequest request) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_KNOWLEDGE_MANAGE);

        Long workspaceId = workspaceContextService.getWorkspaceId();
        KnowledgeSourceJpaEntity entity = new KnowledgeSourceJpaEntity();
        entity.setId(generateId(request.title()));
        entity.setWorkspaceId(workspaceId);
        entity.setTitle(requiredText(request.title(), "title"));
        entity.setSourceType(requiredText(request.sourceType(), "sourceType"));
        entity.setProjectId(validateProject(request.projectId(), workspaceId));
        entity.setSourceUri(normalizeOptional(request.sourceUri()));
        entity.setDocumentCount(request.documentCount() == null ? 0 : request.documentCount());
        entity.setEnabledForAgents(request.enabledForAgents() == null || request.enabledForAgents());
        entity.setStatusLabel(defaulted(request.statusLabel(), "Indexado"));
        entity.setAvailability(defaulted(request.availability(), "live"));
        entity.setNote(requiredText(request.note(), "note"));
        entity.setLastIndexedAt(LocalDateTime.now());

        KnowledgeSourceJpaEntity savedEntity = knowledgeSourceRepository.save(entity);
        auditLogService.record(
                "knowledge_source",
                savedEntity.getId(),
                "created",
                Map.of(
                        "workspaceId", workspaceId,
                        "projectId", savedEntity.getProjectId() == null ? "none" : savedEntity.getProjectId(),
                        "sourceType", savedEntity.getSourceType(),
                        "enabledForAgents", savedEntity.getEnabledForAgents(),
                        "documentCount", savedEntity.getDocumentCount()
                )
        );
        return toResponse(savedEntity, projectNames(workspaceId));
    }

    @Transactional
    public KnowledgeSourceResponse updateSource(String id, UpdateKnowledgeSourceRequest request) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_KNOWLEDGE_MANAGE);

        Long workspaceId = workspaceContextService.getWorkspaceId();
        KnowledgeSourceJpaEntity entity = findSource(id, workspaceId);
        boolean shouldRefreshIndexTimestamp = false;

        if (request.title() != null) {
            entity.setTitle(requiredText(request.title(), "title"));
        }
        if (request.sourceType() != null) {
            entity.setSourceType(requiredText(request.sourceType(), "sourceType"));
            shouldRefreshIndexTimestamp = true;
        }
        if (request.projectId() != null) {
            entity.setProjectId(validateProject(request.projectId(), workspaceId));
        }
        if (request.sourceUri() != null) {
            entity.setSourceUri(normalizeOptional(request.sourceUri()));
            shouldRefreshIndexTimestamp = true;
        }
        if (request.documentCount() != null) {
            entity.setDocumentCount(request.documentCount());
            shouldRefreshIndexTimestamp = true;
        }
        if (request.enabledForAgents() != null) {
            entity.setEnabledForAgents(request.enabledForAgents());
        }
        if (request.statusLabel() != null) {
            entity.setStatusLabel(requiredText(request.statusLabel(), "statusLabel"));
            shouldRefreshIndexTimestamp = true;
        }
        if (request.availability() != null) {
            entity.setAvailability(requiredText(request.availability(), "availability"));
        }
        if (request.note() != null) {
            entity.setNote(requiredText(request.note(), "note"));
        }
        if (shouldRefreshIndexTimestamp) {
            entity.setLastIndexedAt(LocalDateTime.now());
        }

        KnowledgeSourceJpaEntity savedEntity = knowledgeSourceRepository.save(entity);
        auditLogService.record(
                "knowledge_source",
                savedEntity.getId(),
                "updated",
                Map.of(
                        "workspaceId", workspaceId,
                        "projectId", savedEntity.getProjectId() == null ? "none" : savedEntity.getProjectId(),
                        "sourceType", savedEntity.getSourceType(),
                        "enabledForAgents", savedEntity.getEnabledForAgents(),
                        "documentCount", savedEntity.getDocumentCount()
                )
        );
        return toResponse(savedEntity, projectNames(workspaceId));
    }

    @Transactional
    public void deleteSource(String id) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_KNOWLEDGE_MANAGE);

        Long workspaceId = workspaceContextService.getWorkspaceId();
        KnowledgeSourceJpaEntity entity = findSource(id, workspaceId);
        knowledgeSourceRepository.delete(entity);
        auditLogService.record(
                "knowledge_source",
                entity.getId(),
                "deleted",
                Map.of(
                        "workspaceId", workspaceId,
                        "title", entity.getTitle(),
                        "sourceType", entity.getSourceType()
                )
        );
    }

    private KnowledgeSourceJpaEntity findSource(String id, Long workspaceId) {
        return knowledgeSourceRepository.findByIdAndWorkspaceId(id, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("KnowledgeSource", id));
    }

    private Map<String, String> projectNames(Long workspaceId) {
        return projectRepository.findByWorkspaceIdOrderByUpdatedAtDesc(workspaceId, PageRequest.of(0, 200)).stream()
                .collect(Collectors.toMap(ProjectJpaEntity::getId, ProjectJpaEntity::getName));
    }

    private boolean matchesQuery(KnowledgeSourceJpaEntity source, String normalizedQuery, Map<String, String> projectNames) {
        String projectName = source.getProjectId() == null ? "" : projectNames.getOrDefault(source.getProjectId(), "");
        String haystack = String.join(
                " ",
                source.getTitle(),
                source.getSourceType(),
                source.getStatusLabel(),
                source.getNote(),
                source.getSourceUri() == null ? "" : source.getSourceUri(),
                projectName
        ).toLowerCase();
        return haystack.contains(normalizedQuery);
    }

    private KnowledgeSourceResponse toResponse(KnowledgeSourceJpaEntity source, Map<String, String> projectNames) {
        return new KnowledgeSourceResponse(
                source.getId(),
                source.getTitle(),
                source.getSourceType(),
                source.getSourceUri(),
                source.getProjectId(),
                source.getProjectId() == null ? null : projectNames.getOrDefault(source.getProjectId(), source.getProjectId()),
                source.getStatusLabel(),
                source.getAvailability(),
                source.getDocumentCount(),
                Boolean.TRUE.equals(source.getEnabledForAgents()),
                source.getNote(),
                formatDateTime(source.getLastIndexedAt()),
                formatDateTime(source.getUpdatedAt())
        );
    }

    private String validateProject(String projectId, Long workspaceId) {
        String normalizedProjectId = normalizeOptional(projectId);
        if (normalizedProjectId == null) {
            return null;
        }
        return projectRepository.findByIdAndWorkspaceId(normalizedProjectId, workspaceId)
                .map(ProjectJpaEntity::getId)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto", normalizedProjectId));
    }

    private String requiredText(String value, String field) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new IllegalArgumentException(field + " deve ser informado.");
        }
        return normalized;
    }

    private String defaulted(String value, String fallback) {
        String normalized = normalizeOptional(value);
        return normalized == null ? fallback : normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.format(DATE_TIME_FORMATTER);
    }

    private String generateId(String title) {
        String base = requiredText(title, "title")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (base.isBlank()) {
            base = "knowledge-source";
        }
        return base + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
