package com.lume.workspace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "workspace_payment_events",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_workspace_payment_event_gateway",
                        columnNames = {"workspace_id", "gateway_event_id"}
                )
        }
)
public class WorkspacePaymentEventJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workspace_id", nullable = false)
    private Long workspaceId;

    @ManyToOne
    @JoinColumn(name = "invoice_id")
    private WorkspaceInvoiceJpaEntity invoice;

    @Column(name = "gateway_event_id", length = 128)
    private String gatewayEventId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "amount_brl", precision = 12, scale = 2)
    private BigDecimal amountBrl;

    @Column(name = "payload_hash", length = 128)
    private String payloadHash;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @PrePersist
    void onCreate() {
        if (occurredAt == null) {
            occurredAt = LocalDateTime.now();
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

    public WorkspaceInvoiceJpaEntity getInvoice() {
        return invoice;
    }

    public void setInvoice(WorkspaceInvoiceJpaEntity invoice) {
        this.invoice = invoice;
    }

    public String getGatewayEventId() {
        return gatewayEventId;
    }

    public void setGatewayEventId(String gatewayEventId) {
        this.gatewayEventId = gatewayEventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getAmountBrl() {
        return amountBrl;
    }

    public void setAmountBrl(BigDecimal amountBrl) {
        this.amountBrl = amountBrl;
    }

    public String getPayloadHash() {
        return payloadHash;
    }

    public void setPayloadHash(String payloadHash) {
        this.payloadHash = payloadHash;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
}
