package com.lume.workspace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "agent_threads")
public class AgentThreadJpaEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "workspace_id", nullable = false)
    private Long workspaceId;

    @Column(name = "agent_profile_id", nullable = false, length = 64)
    private String agentProfileId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "status_label", nullable = false, length = 80)
    private String statusLabel;

    @Column(nullable = false, length = 32)
    private String availability;

    @Column(name = "last_message_preview", columnDefinition = "TEXT")
    private String lastMessagePreview;

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

    public String getLastMessagePreview() {
        return lastMessagePreview;
    }

    public void setLastMessagePreview(String lastMessagePreview) {
        this.lastMessagePreview = lastMessagePreview;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
