package com.lume.workspace.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lume.workspace.entity.AuditLogJpaEntity;
import com.lume.workspace.repository.AuditLogJpaRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {

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
        AuditLogJpaEntity entity = new AuditLogJpaEntity();
        entity.setOrganizationId(workspaceContextService.getOrganizationId());
        entity.setWorkspaceId(workspaceContextService.getWorkspaceId());
        entity.setActorUserId(workspaceContextService.getActorUserIdOrNull());
        entity.setEntityType(entityType);
        entity.setEntityId(entityId);
        entity.setAction(action);
        entity.setPayload(serializePayload(payload));
        auditLogRepository.save(entity);
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
