package com.lume.workspace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "knowledge_sources")
public class KnowledgeSourceJpaEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "workspace_id", nullable = false)
    private Long workspaceId;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(name = "source_type", nullable = false, length = 80)
    private String sourceType;

    @Column(name = "project_id", length = 64)
    private String projectId;

    @Column(name = "source_uri", length = 512)
    private String sourceUri;

    @Column(name = "document_count", nullable = false)
    private Integer documentCount;

    @Column(name = "enabled_for_agents", nullable = false)
    private Boolean enabledForAgents;

    @Column(name = "status_label", nullable = false, length = 80)
    private String statusLabel;

    @Column(nullable = false, length = 32)
    private String availability;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String note;

    @Column(name = "last_indexed_at")
    private LocalDateTime lastIndexedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (documentCount == null) {
            documentCount = 0;
        }
        if (enabledForAgents == null) {
            enabledForAgents = Boolean.TRUE;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(Long workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public void setStatusLabel(String statusLabel) {
        this.statusLabel = statusLabel;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getSourceUri() {
        return sourceUri;
    }

    public void setSourceUri(String sourceUri) {
        this.sourceUri = sourceUri;
    }

    public Integer getDocumentCount() {
        return documentCount;
    }

    public void setDocumentCount(Integer documentCount) {
        this.documentCount = documentCount;
    }

    public Boolean getEnabledForAgents() {
        return enabledForAgents;
    }

    public void setEnabledForAgents(Boolean enabledForAgents) {
        this.enabledForAgents = enabledForAgents;
    }

    public String getAvailability() {
        return availability;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getLastIndexedAt() {
        return lastIndexedAt;
    }

    public void setLastIndexedAt(LocalDateTime lastIndexedAt) {
        this.lastIndexedAt = lastIndexedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
