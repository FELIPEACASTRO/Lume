package com.lume.workspace.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "workspace_budgets")
public class WorkspaceBudgetJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workspace_id", nullable = false, unique = true)
    private Long workspaceId;

    @Column(name = "cost_center", nullable = false, length = 120)
    private String costCenter = "core_now";

    @Column(name = "chargeback_mode", nullable = false, length = 32)
    private String chargebackMode = "showback";

    @Column(name = "soft_limit_credits", nullable = false)
    private int softLimitCredits = 300;

    @Column(name = "hard_limit_credits", nullable = false)
    private int hardLimitCredits = 450;

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

    public String getCostCenter() {
        return costCenter;
    }

    public void setCostCenter(String costCenter) {
        this.costCenter = costCenter;
    }

    public String getChargebackMode() {
        return chargebackMode;
    }

    public void setChargebackMode(String chargebackMode) {
        this.chargebackMode = chargebackMode;
    }

    public int getSoftLimitCredits() {
        return softLimitCredits;
    }

    public void setSoftLimitCredits(int softLimitCredits) {
        this.softLimitCredits = softLimitCredits;
    }

    public int getHardLimitCredits() {
        return hardLimitCredits;
    }

    public void setHardLimitCredits(int hardLimitCredits) {
        this.hardLimitCredits = hardLimitCredits;
    }
}
