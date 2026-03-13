package com.lume.workspace.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lume.workspace.dto.AuditFeedEntryResponse;
import com.lume.workspace.entity.AuditLogJpaEntity;
import com.lume.workspace.repository.AuditLogJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AuditLogService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final AuditLogJpaRepository auditLogRepository;
    private final WorkspaceContextService workspaceContextService;
    private final ObjectMapper objectMapper;

    public AuditLogService(
            AuditLogJpaRepository auditLogRepository,
            WorkspaceContextService workspaceContextService,
            ObjectMapper objectMapper
    ) {
        this.auditLogRepository = auditLogRepository;
        this.workspaceContextService = workspaceContextService;
        this.objectMapper = objectMapper;
    }

    public void record(String entityType, String entityId, String action, Object payload) {
        recordExplicit(
                workspaceContextService.getOrganizationId(),
                workspaceContextService.getWorkspaceId(),
                workspaceContextService.getActorUserIdOrNull(),
                entityType,
                entityId,
                action,
                payload
        );
    }

    @Transactional
    public void recordExplicit(
            Long organizationId,
            Long workspaceId,
            Long actorUserId,
            String entityType,
            String entityId,
            String action,
            Object payload
    ) {
        AuditLogJpaEntity entity = new AuditLogJpaEntity();
        entity.setOrganizationId(organizationId);
        entity.setWorkspaceId(workspaceId);
        entity.setActorUserId(actorUserId);
        entity.setEntityType(entityType);
        entity.setEntityId(entityId);
        entity.setAction(action);
        entity.setPayload(serializePayload(payload));
        auditLogRepository.save(entity);
    }

    public List<AuditFeedEntryResponse> listCurrentWorkspace(int limit) {
        int safeLimit = limit <= 0 ? 20 : Math.min(limit, 200);
        return auditLogRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceContextService.getWorkspaceId(), PageRequest.of(0, safeLimit)).stream()
                .map(item -> new AuditFeedEntryResponse(
                        item.getId(),
                        item.getActorUserId(),
                        item.getEntityType(),
                        item.getEntityId(),
                        item.getAction(),
                        item.getPayload(),
                        item.getCreatedAt() == null ? null : DATE_TIME_FORMATTER.format(item.getCreatedAt())
                ))
                .toList();
    }

    private String serializePayload(Object payload) {
        if (payload == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            return String.valueOf(payload);
        }
    }
}
