package com.lume.workspace.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_preferences")
public class UserPreferenceJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false, length = 32)
    private String appearance = "system";

    @Column(name = "language_code", nullable = false, length = 20)
    private String languageCode = "pt-BR";

    @Column(name = "email_updates", nullable = false)
    private boolean emailUpdates = true;

    @Column(name = "product_updates", nullable = false)
    private boolean productUpdates = true;

    @Column(name = "active_workspace_id")
    private Long activeWorkspaceId;

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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getAppearance() {
        return appearance;
    }

    public void setAppearance(String appearance) {
        this.appearance = appearance;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    public boolean isEmailUpdates() {
        return emailUpdates;
    }

    public void setEmailUpdates(boolean emailUpdates) {
        this.emailUpdates = emailUpdates;
    }

    public boolean isProductUpdates() {
        return productUpdates;
    }

    public void setProductUpdates(boolean productUpdates) {
        this.productUpdates = productUpdates;
    }

    public Long getActiveWorkspaceId() {
        return activeWorkspaceId;
    }

    public void setActiveWorkspaceId(Long activeWorkspaceId) {
        this.activeWorkspaceId = activeWorkspaceId;
    }
}
