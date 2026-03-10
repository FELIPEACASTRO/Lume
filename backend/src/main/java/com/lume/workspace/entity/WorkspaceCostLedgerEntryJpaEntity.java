package com.lume.workspace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "workspace_cost_ledger_entries")
public class WorkspaceCostLedgerEntryJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workspace_id")
    private Long workspaceId;

    @Column(name = "provider_code", nullable = false, length = 64)
    private String providerCode;

    @Column(name = "model_code", length = 128)
    private String modelCode;

    @Column(name = "capability", nullable = false, length = 64)
    private String capability;

    @Column(name = "request_id", length = 128)
    private String requestId;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "estimated_input_tokens")
    private Integer estimatedInputTokens;

    @Column(name = "estimated_output_tokens")
    private Integer estimatedOutputTokens;

    @Column(name = "estimated_cost_usd")
    private Double estimatedCostUsd;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "fallback_used", nullable = false)
    private boolean fallbackUsed;

    @Column(name = "routing_mode", length = 32)
    private String routingMode;

    @Column(name = "policy_decision_summary", length = 255)
    private String policyDecisionSummary;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(Long workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public void setProviderCode(String providerCode) {
        this.providerCode = providerCode;
    }

    public String getModelCode() {
        return modelCode;
    }

    public void setModelCode(String modelCode) {
        this.modelCode = modelCode;
    }

    public String getCapability() {
        return capability;
    }

    public void setCapability(String capability) {
        this.capability = capability;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getEstimatedInputTokens() {
        return estimatedInputTokens;
    }

    public void setEstimatedInputTokens(Integer estimatedInputTokens) {
        this.estimatedInputTokens = estimatedInputTokens;
    }

    public Integer getEstimatedOutputTokens() {
        return estimatedOutputTokens;
    }

    public void setEstimatedOutputTokens(Integer estimatedOutputTokens) {
        this.estimatedOutputTokens = estimatedOutputTokens;
    }

    public Double getEstimatedCostUsd() {
        return estimatedCostUsd;
    }

    public void setEstimatedCostUsd(Double estimatedCostUsd) {
        this.estimatedCostUsd = estimatedCostUsd;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public boolean isFallbackUsed() {
        return fallbackUsed;
    }

    public void setFallbackUsed(boolean fallbackUsed) {
        this.fallbackUsed = fallbackUsed;
    }

    public String getRoutingMode() {
        return routingMode;
    }

    public void setRoutingMode(String routingMode) {
        this.routingMode = routingMode;
    }

    public String getPolicyDecisionSummary() {
        return policyDecisionSummary;
    }

    public void setPolicyDecisionSummary(String policyDecisionSummary) {
        this.policyDecisionSummary = policyDecisionSummary;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
