package com.lume.workspace.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLogJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "workspace_id", nullable = false)
    private Long workspaceId;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "entity_type", nullable = false, length = 80)
    private String entityType;

    @Column(name = "entity_id", nullable = false, length = 80)
    private String entityId;

    @Column(nullable = false, length = 80)
    private String action;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    public void setWorkspaceId(Long workspaceId) {
        this.workspaceId = workspaceId;
    }

    public void setActorUserId(Long actorUserId) {
        this.actorUserId = actorUserId;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}
