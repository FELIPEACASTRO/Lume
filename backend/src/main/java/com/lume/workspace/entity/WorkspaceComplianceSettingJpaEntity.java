package com.lume.workspace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "workspace_compliance_settings")
public class WorkspaceComplianceSettingJpaEntity {

    @Id
    @Column(name = "workspace_id")
    private Long workspaceId;

    @Column(name = "retention_policy_status", nullable = false, length = 32)
    private String retentionPolicyStatus;

    @Column(name = "retention_days")
    private Integer retentionDays;

    @Column(name = "access_review_status", nullable = false, length = 32)
    private String accessReviewStatus;

    @Column(name = "access_review_frequency_days")
    private Integer accessReviewFrequencyDays;

    @Column(name = "consent_tracking_enabled", nullable = false)
    private boolean consentTrackingEnabled;

    @Column(name = "terms_version", length = 64)
    private String termsVersion;

    @Column(name = "updated_by_user_id")
    private Long updatedByUserId;

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

    public String getRetentionPolicyStatus() {
        return retentionPolicyStatus;
    }

    public void setRetentionPolicyStatus(String retentionPolicyStatus) {
        this.retentionPolicyStatus = retentionPolicyStatus;
    }

    public Integer getRetentionDays() {
        return retentionDays;
    }

    public void setRetentionDays(Integer retentionDays) {
        this.retentionDays = retentionDays;
    }

    public String getAccessReviewStatus() {
        return accessReviewStatus;
    }

    public void setAccessReviewStatus(String accessReviewStatus) {
        this.accessReviewStatus = accessReviewStatus;
    }

    public Integer getAccessReviewFrequencyDays() {
        return accessReviewFrequencyDays;
    }

    public void setAccessReviewFrequencyDays(Integer accessReviewFrequencyDays) {
        this.accessReviewFrequencyDays = accessReviewFrequencyDays;
    }

    public boolean isConsentTrackingEnabled() {
        return consentTrackingEnabled;
    }

    public void setConsentTrackingEnabled(boolean consentTrackingEnabled) {
        this.consentTrackingEnabled = consentTrackingEnabled;
    }

    public String getTermsVersion() {
        return termsVersion;
    }

    public void setTermsVersion(String termsVersion) {
        this.termsVersion = termsVersion;
    }

    public Long getUpdatedByUserId() {
        return updatedByUserId;
    }

    public void setUpdatedByUserId(Long updatedByUserId) {
        this.updatedByUserId = updatedByUserId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
