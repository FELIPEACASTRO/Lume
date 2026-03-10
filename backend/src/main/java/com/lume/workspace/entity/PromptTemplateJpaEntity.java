package com.lume.workspace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "prompt_templates")
public class PromptTemplateJpaEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "workspace_id", nullable = false)
    private Long workspaceId;

    @Column(name = "project_id", length = 64)
    private String projectId;

    @Column(name = "agent_profile_id", length = 64)
    private String agentProfileId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(name = "prompt_body", nullable = false, columnDefinition = "TEXT")
    private String promptBody;

    @Column(name = "template_scope", nullable = false, length = 64)
    private String templateScope;

    @Column(name = "status_label", nullable = false, length = 80)
    private String statusLabel;

    @Column(nullable = false, length = 32)
    private String availability;

    @Column(name = "owner_name", nullable = false, length = 120)
    private String ownerName;

    @Column(name = "variables_raw", nullable = false, columnDefinition = "TEXT")
    private String variablesRaw;

    @Column(nullable = false)
    private boolean favorited;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (templateScope == null || templateScope.isBlank()) {
            templateScope = "workspace";
        }
        if (variablesRaw == null) {
            variablesRaw = "";
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
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

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getAgentProfileId() {
        return agentProfileId;
    }

    public void setAgentProfileId(String agentProfileId) {
        this.agentProfileId = agentProfileId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getPromptBody() {
        return promptBody;
    }

    public void setPromptBody(String promptBody) {
        this.promptBody = promptBody;
    }

    public String getTemplateScope() {
        return templateScope;
    }

    public void setTemplateScope(String templateScope) {
        this.templateScope = templateScope;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public void setStatusLabel(String statusLabel) {
        this.statusLabel = statusLabel;
    }

    public String getAvailability() {
        return availability;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getVariablesRaw() {
        return variablesRaw;
    }

    public void setVariablesRaw(String variablesRaw) {
        this.variablesRaw = variablesRaw;
    }

    public boolean isFavorited() {
        return favorited;
    }

    public void setFavorited(boolean favorited) {
        this.favorited = favorited;
    }

    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(LocalDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
