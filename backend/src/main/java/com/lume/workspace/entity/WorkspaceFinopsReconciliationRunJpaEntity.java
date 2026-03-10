package com.lume.workspace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "workspace_finops_reconciliation_runs")
public class WorkspaceFinopsReconciliationRunJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workspace_id", nullable = false)
    private Long workspaceId;

    @Column(name = "run_mode", nullable = false, length = 32)
    private String runMode;

    @Column(name = "reconciliation_status", nullable = false, length = 64)
    private String reconciliationStatus;

    @Column(name = "subscription_credits", nullable = false)
    private int subscriptionCredits;

    @Column(name = "ledger_credit_balance", nullable = false)
    private int ledgerCreditBalance;

    @Column(name = "credit_drift", nullable = false)
    private int creditDrift;

    @Column(name = "invoices_total", nullable = false)
    private long invoicesTotal;

    @Column(name = "invoices_paid", nullable = false)
    private long invoicesPaid;

    @Column(name = "invoices_paid_amount_brl", nullable = false, precision = 12, scale = 2)
    private BigDecimal invoicesPaidAmountBrl;

    @Column(name = "payment_events_total", nullable = false)
    private long paymentEventsTotal;

    @Column(name = "payment_events_processed", nullable = false)
    private long paymentEventsProcessed;

    @Column(name = "payment_events_processed_amount_brl", nullable = false, precision = 12, scale = 2)
    private BigDecimal paymentEventsProcessedAmountBrl;

    @Column(name = "orphan_payment_events", nullable = false)
    private long orphanPaymentEvents;

    @Column(name = "pending_payment_events", nullable = false)
    private long pendingPaymentEvents;

    @Column(name = "credit_fix_applied", nullable = false)
    private boolean creditFixApplied;

    @Column(name = "credit_fix_delta")
    private Integer creditFixDelta;

    @Column(name = "recommendation", length = 255)
    private String recommendation;

    @Column(name = "executed_at", nullable = false, updatable = false)
    private LocalDateTime executedAt;

    @PrePersist
    void onCreate() {
        if (executedAt == null) {
            executedAt = LocalDateTime.now();
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

    public String getRunMode() {
        return runMode;
    }

    public void setRunMode(String runMode) {
        this.runMode = runMode;
    }

    public String getReconciliationStatus() {
        return reconciliationStatus;
    }

    public void setReconciliationStatus(String reconciliationStatus) {
        this.reconciliationStatus = reconciliationStatus;
    }

    public int getSubscriptionCredits() {
        return subscriptionCredits;
    }

    public void setSubscriptionCredits(int subscriptionCredits) {
        this.subscriptionCredits = subscriptionCredits;
    }

    public int getLedgerCreditBalance() {
        return ledgerCreditBalance;
    }

    public void setLedgerCreditBalance(int ledgerCreditBalance) {
        this.ledgerCreditBalance = ledgerCreditBalance;
    }

    public int getCreditDrift() {
        return creditDrift;
    }

    public void setCreditDrift(int creditDrift) {
        this.creditDrift = creditDrift;
    }

    public long getInvoicesTotal() {
        return invoicesTotal;
    }

    public void setInvoicesTotal(long invoicesTotal) {
        this.invoicesTotal = invoicesTotal;
    }

    public long getInvoicesPaid() {
        return invoicesPaid;
    }

    public void setInvoicesPaid(long invoicesPaid) {
        this.invoicesPaid = invoicesPaid;
    }

    public BigDecimal getInvoicesPaidAmountBrl() {
        return invoicesPaidAmountBrl;
    }

    public void setInvoicesPaidAmountBrl(BigDecimal invoicesPaidAmountBrl) {
        this.invoicesPaidAmountBrl = invoicesPaidAmountBrl;
    }

    public long getPaymentEventsTotal() {
        return paymentEventsTotal;
    }

    public void setPaymentEventsTotal(long paymentEventsTotal) {
        this.paymentEventsTotal = paymentEventsTotal;
    }

    public long getPaymentEventsProcessed() {
        return paymentEventsProcessed;
    }

    public void setPaymentEventsProcessed(long paymentEventsProcessed) {
        this.paymentEventsProcessed = paymentEventsProcessed;
    }

    public BigDecimal getPaymentEventsProcessedAmountBrl() {
        return paymentEventsProcessedAmountBrl;
    }

    public void setPaymentEventsProcessedAmountBrl(BigDecimal paymentEventsProcessedAmountBrl) {
        this.paymentEventsProcessedAmountBrl = paymentEventsProcessedAmountBrl;
    }

    public long getOrphanPaymentEvents() {
        return orphanPaymentEvents;
    }

    public void setOrphanPaymentEvents(long orphanPaymentEvents) {
        this.orphanPaymentEvents = orphanPaymentEvents;
    }

    public long getPendingPaymentEvents() {
        return pendingPaymentEvents;
    }

    public void setPendingPaymentEvents(long pendingPaymentEvents) {
        this.pendingPaymentEvents = pendingPaymentEvents;
    }

    public boolean isCreditFixApplied() {
        return creditFixApplied;
    }

    public void setCreditFixApplied(boolean creditFixApplied) {
        this.creditFixApplied = creditFixApplied;
    }

    public Integer getCreditFixDelta() {
        return creditFixDelta;
    }

    public void setCreditFixDelta(Integer creditFixDelta) {
        this.creditFixDelta = creditFixDelta;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(LocalDateTime executedAt) {
        this.executedAt = executedAt;
    }
}
