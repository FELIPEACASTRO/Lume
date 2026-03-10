package com.lume.workspace.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "workspace_subscriptions")
public class WorkspaceSubscriptionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workspace_id", nullable = false, unique = true)
    private Long workspaceId;

    @Column(name = "plan_code", nullable = false, length = 32)
    private String planCode = "starter";

    @Column(name = "plan_label", nullable = false, length = 64)
    private String planLabel = "Starter";

    @Column(name = "subscription_status", nullable = false, length = 32)
    private String subscriptionStatus = "active";

    @Column(name = "billing_interval", nullable = false, length = 32)
    private String billingInterval = "monthly";

    @Column(name = "included_credits", nullable = false)
    private int includedCredits = 500;

    @Column(name = "extra_credits", nullable = false)
    private int extraCredits = 0;

    @Column(name = "renews_at")
    private LocalDate renewsAt;

    @Column(name = "commercial_note", nullable = false, length = 255)
    private String commercialNote = "Base comercial inicial do workspace. Billing externo e overage entram em uma trilha posterior.";

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

    public String getPlanCode() {
        return planCode;
    }

    public void setPlanCode(String planCode) {
        this.planCode = planCode;
    }

    public String getPlanLabel() {
        return planLabel;
    }

    public void setPlanLabel(String planLabel) {
        this.planLabel = planLabel;
    }

    public String getSubscriptionStatus() {
        return subscriptionStatus;
    }

    public void setSubscriptionStatus(String subscriptionStatus) {
        this.subscriptionStatus = subscriptionStatus;
    }

    public String getBillingInterval() {
        return billingInterval;
    }

    public void setBillingInterval(String billingInterval) {
        this.billingInterval = billingInterval;
    }

    public int getIncludedCredits() {
        return includedCredits;
    }

    public void setIncludedCredits(int includedCredits) {
        this.includedCredits = includedCredits;
    }

    public int getExtraCredits() {
        return extraCredits;
    }

    public void setExtraCredits(int extraCredits) {
        this.extraCredits = extraCredits;
    }

    public LocalDate getRenewsAt() {
        return renewsAt;
    }

    public void setRenewsAt(LocalDate renewsAt) {
        this.renewsAt = renewsAt;
    }

    public String getCommercialNote() {
        return commercialNote;
    }

    public void setCommercialNote(String commercialNote) {
        this.commercialNote = commercialNote;
    }
}
