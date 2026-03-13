package com.lume.workspace.service;

import com.lume.workspace.dto.FinopsReconciliationRunEntryResponse;
import com.lume.workspace.dto.FinopsReconciliationResponse;
import com.lume.workspace.dto.FinopsReconciliationRunRequest;
import com.lume.workspace.entity.WorkspaceFinopsReconciliationRunJpaEntity;
import com.lume.workspace.entity.WorkspaceSubscriptionJpaEntity;
import com.lume.workspace.repository.WorkspaceCreditLedgerEntryJpaRepository;
import com.lume.workspace.repository.WorkspaceFinopsReconciliationRunJpaRepository;
import com.lume.workspace.repository.WorkspaceInvoiceJpaRepository;
import com.lume.workspace.repository.WorkspacePaymentEventJpaRepository;
import com.lume.workspace.repository.WorkspaceSubscriptionJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class WorkspaceFinopsReconciliationService implements WorkspaceFinopsReconciliationExecutor {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final String STATUS_PAID = "paid";
    private static final String STATUS_PROCESSED = "processed";
    private static final String STATUS_PENDING = "pending";
    private static final String RUN_MODE_MANUAL = "manual";
    private static final String RUN_MODE_SYSTEM = "system";

    private final WorkspaceContextService workspaceContextService;
    private final WorkspaceSubscriptionJpaRepository subscriptionRepository;
    private final WorkspaceCreditLedgerEntryJpaRepository creditLedgerRepository;
    private final WorkspaceInvoiceJpaRepository invoiceRepository;
    private final WorkspacePaymentEventJpaRepository paymentEventRepository;
    private final WorkspaceFinopsReconciliationRunJpaRepository reconciliationRunRepository;
    private final WorkspaceLedgerService workspaceLedgerService;

    public WorkspaceFinopsReconciliationService(
            WorkspaceContextService workspaceContextService,
            WorkspaceSubscriptionJpaRepository subscriptionRepository,
            WorkspaceCreditLedgerEntryJpaRepository creditLedgerRepository,
            WorkspaceInvoiceJpaRepository invoiceRepository,
            WorkspacePaymentEventJpaRepository paymentEventRepository,
            WorkspaceFinopsReconciliationRunJpaRepository reconciliationRunRepository,
            WorkspaceLedgerService workspaceLedgerService
    ) {
        this.workspaceContextService = workspaceContextService;
        this.subscriptionRepository = subscriptionRepository;
        this.creditLedgerRepository = creditLedgerRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentEventRepository = paymentEventRepository;
        this.reconciliationRunRepository = reconciliationRunRepository;
        this.workspaceLedgerService = workspaceLedgerService;
    }

    @Transactional(readOnly = true)
    public FinopsReconciliationResponse previewCurrentWorkspace() {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_BUDGETS_READ);
        return reconcile(
                workspaceContextService.getWorkspaceId(),
                false,
                false,
                null,
                null
        );
    }

    @Transactional
    public FinopsReconciliationResponse runCurrentWorkspace(FinopsReconciliationRunRequest request) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_BUDGETS_MANAGE);
        boolean applyCreditFix = request != null && Boolean.TRUE.equals(request.applyCreditFix());
        return reconcile(
                workspaceContextService.getWorkspaceId(),
                applyCreditFix,
                true,
                RUN_MODE_MANUAL,
                workspaceContextService.getActorUserIdOrNull()
        );
    }

    @Transactional
    @Override
    public FinopsReconciliationResponse runForWorkspaceSystem(Long workspaceId, boolean applyCreditFix) {
        if (workspaceId == null) {
            throw new IllegalArgumentException("workspaceId e obrigatorio para reconciliacao de sistema.");
        }
        return reconcile(workspaceId, applyCreditFix, true, RUN_MODE_SYSTEM, null);
    }

    @Transactional(readOnly = true)
    public List<FinopsReconciliationRunEntryResponse> listCurrentWorkspaceHistory(int limit) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_BUDGETS_READ);
        return reconciliationRunRepository.findByWorkspaceIdOrderByExecutedAtDesc(
                workspaceContextService.getWorkspaceId(), PageRequest.of(0, safeLimit(limit)))
                .stream()
                .map(item -> new FinopsReconciliationRunEntryResponse(
                        item.getId(),
                        item.getRunMode(),
                        item.getReconciliationStatus(),
                        item.getSubscriptionCredits(),
                        item.getLedgerCreditBalance(),
                        item.getCreditDrift(),
                        item.getInvoicesTotal(),
                        item.getInvoicesPaid(),
                        item.getInvoicesPaidAmountBrl(),
                        item.getPaymentEventsTotal(),
                        item.getPaymentEventsProcessed(),
                        item.getPaymentEventsProcessedAmountBrl(),
                        item.getOrphanPaymentEvents(),
                        item.getPendingPaymentEvents(),
                        item.isCreditFixApplied(),
                        item.getCreditFixDelta(),
                        item.getRecommendation(),
                        DATE_TIME_FORMATTER.format(item.getExecutedAt())
                ))
                .toList();
    }

    private FinopsReconciliationResponse reconcile(
            Long workspaceId,
            boolean applyCreditFix,
            boolean recordRunEvent,
            String runMode,
            Long actorUserId
    ) {
        LocalDateTime executedAt = LocalDateTime.now();

        WorkspaceSubscriptionJpaEntity subscription = subscriptionRepository.findByWorkspaceId(workspaceId).orElse(null);
        int subscriptionCredits = subscription == null ? 0 : subscription.getIncludedCredits() + subscription.getExtraCredits();
        int ledgerCreditBalance = creditLedgerRepository.sumCreditsByWorkspaceId(workspaceId);
        int creditDrift = subscriptionCredits - ledgerCreditBalance;

        boolean creditFixApplied = false;
        Integer creditFixDelta = null;
        if (applyCreditFix && creditDrift != 0) {
            workspaceLedgerService.recordCreditEntry(
                    workspaceId,
                    creditDrift > 0 ? "reconciliation_credit_grant" : "reconciliation_credit_debit",
                    "finops_reconciliation",
                    "workspace:" + workspaceId,
                    creditDrift,
                    "Ajuste automatico de reconciliacao entre assinatura comercial e credit ledger."
            );
            creditFixApplied = true;
            creditFixDelta = creditDrift;
            ledgerCreditBalance += creditDrift;
            creditDrift = 0;
        }

        long invoicesTotal = invoiceRepository.countByWorkspaceId(workspaceId);
        long invoicesPaid = invoiceRepository.countByWorkspaceIdAndStatus(workspaceId, STATUS_PAID);
        BigDecimal invoicesPaidAmount = safeMoney(invoiceRepository.sumAmountByWorkspaceIdAndStatus(workspaceId, STATUS_PAID));

        long paymentEventsTotal = paymentEventRepository.countByWorkspaceId(workspaceId);
        long paymentEventsProcessed = paymentEventRepository.countByWorkspaceIdAndStatus(workspaceId, STATUS_PROCESSED);
        BigDecimal paymentEventsProcessedAmount = safeMoney(paymentEventRepository.sumAmountByWorkspaceIdAndStatus(workspaceId, STATUS_PROCESSED));
        long orphanPaymentEvents = paymentEventRepository.countByWorkspaceIdAndInvoiceIsNull(workspaceId);
        long pendingPaymentEvents = paymentEventRepository.countByWorkspaceIdAndStatusAndProcessedAtIsNull(workspaceId, STATUS_PENDING);

        BigDecimal amountDrift = paymentEventsProcessedAmount.subtract(invoicesPaidAmount).setScale(2, RoundingMode.HALF_UP);

        String status = reconciliationStatus(subscription, creditDrift, orphanPaymentEvents, pendingPaymentEvents, amountDrift);
        String recommendation = recommendation(subscription, creditDrift, orphanPaymentEvents, pendingPaymentEvents, amountDrift);

        if (recordRunEvent) {
            workspaceLedgerService.recordUsageEventForWorkspace(
                    workspaceId,
                    actorUserId,
                    "finops.reconciliation.run",
                    "workspace_finops",
                    String.valueOf(workspaceId),
                    "Status=" + status + ", creditDrift=" + creditDrift + ", orphanPayments=" + orphanPaymentEvents
            );
            persistHistory(
                    workspaceId,
                    runMode == null ? RUN_MODE_MANUAL : runMode,
                    status,
                    subscriptionCredits,
                    ledgerCreditBalance,
                    creditDrift,
                    invoicesTotal,
                    invoicesPaid,
                    invoicesPaidAmount,
                    paymentEventsTotal,
                    paymentEventsProcessed,
                    paymentEventsProcessedAmount,
                    orphanPaymentEvents,
                    pendingPaymentEvents,
                    creditFixApplied,
                    creditFixDelta,
                    recommendation,
                    executedAt
            );
        }

        return new FinopsReconciliationResponse(
                status,
                subscriptionCredits,
                ledgerCreditBalance,
                creditDrift,
                invoicesTotal,
                invoicesPaid,
                invoicesPaidAmount,
                paymentEventsTotal,
                paymentEventsProcessed,
                paymentEventsProcessedAmount,
                orphanPaymentEvents,
                pendingPaymentEvents,
                creditFixApplied,
                creditFixDelta,
                recommendation,
                DATE_TIME_FORMATTER.format(executedAt)
        );
    }

    private void persistHistory(
            Long workspaceId,
            String runMode,
            String status,
            int subscriptionCredits,
            int ledgerCreditBalance,
            int creditDrift,
            long invoicesTotal,
            long invoicesPaid,
            BigDecimal invoicesPaidAmount,
            long paymentEventsTotal,
            long paymentEventsProcessed,
            BigDecimal paymentEventsProcessedAmount,
            long orphanPaymentEvents,
            long pendingPaymentEvents,
            boolean creditFixApplied,
            Integer creditFixDelta,
            String recommendation,
            LocalDateTime executedAt
    ) {
        WorkspaceFinopsReconciliationRunJpaEntity run = new WorkspaceFinopsReconciliationRunJpaEntity();
        run.setWorkspaceId(workspaceId);
        run.setRunMode(runMode);
        run.setReconciliationStatus(status);
        run.setSubscriptionCredits(subscriptionCredits);
        run.setLedgerCreditBalance(ledgerCreditBalance);
        run.setCreditDrift(creditDrift);
        run.setInvoicesTotal(invoicesTotal);
        run.setInvoicesPaid(invoicesPaid);
        run.setInvoicesPaidAmountBrl(invoicesPaidAmount);
        run.setPaymentEventsTotal(paymentEventsTotal);
        run.setPaymentEventsProcessed(paymentEventsProcessed);
        run.setPaymentEventsProcessedAmountBrl(paymentEventsProcessedAmount);
        run.setOrphanPaymentEvents(orphanPaymentEvents);
        run.setPendingPaymentEvents(pendingPaymentEvents);
        run.setCreditFixApplied(creditFixApplied);
        run.setCreditFixDelta(creditFixDelta);
        run.setRecommendation(recommendation);
        run.setExecutedAt(executedAt);
        reconciliationRunRepository.save(run);
    }

    private String reconciliationStatus(
            WorkspaceSubscriptionJpaEntity subscription,
            int creditDrift,
            long orphanPaymentEvents,
            long pendingPaymentEvents,
            BigDecimal amountDrift
    ) {
        if (subscription == null) {
            return "missing_subscription";
        }
        if (creditDrift == 0
                && orphanPaymentEvents == 0
                && pendingPaymentEvents == 0
                && amountDrift.compareTo(BigDecimal.ZERO) == 0) {
            return "balanced";
        }
        return "attention_required";
    }

    private String recommendation(
            WorkspaceSubscriptionJpaEntity subscription,
            int creditDrift,
            long orphanPaymentEvents,
            long pendingPaymentEvents,
            BigDecimal amountDrift
    ) {
        if (subscription == null) {
            return "Workspace sem assinatura comercial ativa. Inicialize billing antes de reconciliar.";
        }
        List<String> recommendations = new ArrayList<>();
        if (creditDrift != 0) {
            recommendations.add("Ajustar drift de creditos entre assinatura e credit ledger.");
        }
        if (orphanPaymentEvents > 0) {
            recommendations.add("Vincular payment events orfaos a invoices validas.");
        }
        if (pendingPaymentEvents > 0) {
            recommendations.add("Reprocessar webhooks pendentes e verificar retries do gateway.");
        }
        if (amountDrift.compareTo(BigDecimal.ZERO) != 0) {
            recommendations.add("Conferir diferenca de valor entre invoices pagas e payment events processados.");
        }
        if (recommendations.isEmpty()) {
            return "Reconciliacao financeira em estado consistente.";
        }
        return String.join(" ", recommendations);
    }

    private BigDecimal safeMoney(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private int safeLimit(int requestedLimit) {
        if (requestedLimit <= 0) {
            return 20;
        }
        return Math.min(requestedLimit, 200);
    }
}
