package com.lume.workspace.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "workspace_onboarding_profiles")
public class WorkspaceOnboardingProfileJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workspace_id", nullable = false, unique = true)
    private Long workspaceId;

    @Column(name = "primary_use_case", nullable = false, length = 64)
    private String primaryUseCase = "operations";

    @Column(name = "work_style", nullable = false, length = 64)
    private String workStyle = "small_team";

    @Column(name = "activation_status", nullable = false, length = 32)
    private String activationStatus = "started";

    @Column(name = "activation_note", nullable = false, length = 255)
    private String activationNote = "Onboarding iniciado. Defina seu perfil para continuar.";

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

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(Long workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getPrimaryUseCase() {
        return primaryUseCase;
    }

    public void setPrimaryUseCase(String primaryUseCase) {
        this.primaryUseCase = primaryUseCase;
    }

    public String getWorkStyle() {
        return workStyle;
    }

    public void setWorkStyle(String workStyle) {
        this.workStyle = workStyle;
    }

    public String getActivationStatus() {
        return activationStatus;
    }

    public void setActivationStatus(String activationStatus) {
        this.activationStatus = activationStatus;
    }

    public String getActivationNote() {
        return activationNote;
    }

    public void setActivationNote(String activationNote) {
        this.activationNote = activationNote;
    }
}
