package com.lume.workspace.service;

import com.lume.infrastructure.config.FinopsAnomalyProperties;
import com.lume.workspace.dto.FinopsAnomalyResponse;
import com.lume.workspace.entity.WorkspaceBudgetJpaEntity;
import com.lume.workspace.entity.WorkspaceSubscriptionJpaEntity;
import com.lume.workspace.repository.WorkspaceBudgetJpaRepository;
import com.lume.workspace.repository.WorkspaceCostLedgerEntryJpaRepository;
import com.lume.workspace.repository.WorkspaceCreditLedgerEntryJpaRepository;
import com.lume.workspace.repository.WorkspacePaymentEventJpaRepository;
import com.lume.workspace.repository.WorkspaceSubscriptionJpaRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("WorkspaceFinopsAnomalyService - Unit Tests")
class WorkspaceFinopsAnomalyServiceTest {

    private static final Long WORKSPACE_ID = 1L;

    private final WorkspaceBudgetJpaRepository budgetRepository = mock(WorkspaceBudgetJpaRepository.class);
    private final WorkspaceCostLedgerEntryJpaRepository costLedgerRepository = mock(WorkspaceCostLedgerEntryJpaRepository.class);
    private final WorkspaceCreditLedgerEntryJpaRepository creditLedgerRepository = mock(WorkspaceCreditLedgerEntryJpaRepository.class);
    private final WorkspacePaymentEventJpaRepository paymentEventRepository = mock(WorkspacePaymentEventJpaRepository.class);
    private final WorkspaceSubscriptionJpaRepository subscriptionRepository = mock(WorkspaceSubscriptionJpaRepository.class);

    private List<FinopsAnomalyDetector> buildDetectors(int consumedCredits, FinopsAnomalyProperties properties) {
        return List.of(
                new BudgetLimitAnomalyDetector(budgetRepository, new StubWorkspaceMeteringService(consumedCredits)),
                new InferenceSuccessRateAnomalyDetector(costLedgerRepository, properties),
                new CostPerRunAnomalyDetector(costLedgerRepository, properties),
                new CreditBalanceAnomalyDetector(creditLedgerRepository),
                new PaymentEventsAnomalyDetector(paymentEventRepository),
                new SubscriptionAnomalyDetector(subscriptionRepository)
        );
    }

    private WorkspaceFinopsAnomalyService createService(int consumedCredits, FinopsAnomalyProperties properties) {
        return new WorkspaceFinopsAnomalyService(
                new StubWorkspaceContextService(),
                buildDetectors(consumedCredits, properties)
        );
    }

    private WorkspaceFinopsAnomalyService createService(int consumedCredits) {
        return createService(consumedCredits, defaultProperties());
    }

    private FinopsAnomalyProperties defaultProperties() {
        return new FinopsAnomalyProperties();
    }

    private void setupHealthyDefaults() {
        when(budgetRepository.findByWorkspaceId(WORKSPACE_ID)).thenReturn(Optional.empty());
        when(costLedgerRepository.countByWorkspaceId(WORKSPACE_ID)).thenReturn(0L);
        when(costLedgerRepository.countByWorkspaceIdAndStatus(WORKSPACE_ID, "completed")).thenReturn(0L);
        when(costLedgerRepository.averageEstimatedCostByWorkspaceId(WORKSPACE_ID)).thenReturn(null);
        when(creditLedgerRepository.sumCreditsByWorkspaceId(WORKSPACE_ID)).thenReturn(100);
        when(paymentEventRepository.countByWorkspaceIdAndStatusAndProcessedAtIsNull(WORKSPACE_ID, "pending")).thenReturn(0L);
        when(paymentEventRepository.countByWorkspaceIdAndInvoiceIsNull(WORKSPACE_ID)).thenReturn(0L);
        when(subscriptionRepository.findByWorkspaceId(WORKSPACE_ID)).thenReturn(Optional.of(new WorkspaceSubscriptionJpaEntity()));
    }

    @Test
    @DisplayName("should detect budget hard limit reached when consumed >= hard limit")
    void shouldDetectBudgetHardLimitReached() {
        setupHealthyDefaults();
        WorkspaceBudgetJpaEntity budget = new WorkspaceBudgetJpaEntity();
        budget.setWorkspaceId(WORKSPACE_ID);
        budget.setSoftLimitCredits(200);
        budget.setHardLimitCredits(400);
        when(budgetRepository.findByWorkspaceId(WORKSPACE_ID)).thenReturn(Optional.of(budget));

        WorkspaceFinopsAnomalyService service = createService(400);
        List<FinopsAnomalyResponse> anomalies = service.listCurrentWorkspace();

        assertThat(anomalies).anyMatch(a ->
                a.code().equals("budget_hard_limit_reached") && a.severity().equals("critical"));
    }

    @Test
    @DisplayName("should detect budget soft limit reached when consumed >= soft and < hard")
    void shouldDetectBudgetSoftLimitReached() {
        setupHealthyDefaults();
        WorkspaceBudgetJpaEntity budget = new WorkspaceBudgetJpaEntity();
        budget.setWorkspaceId(WORKSPACE_ID);
        budget.setSoftLimitCredits(200);
        budget.setHardLimitCredits(400);
        when(budgetRepository.findByWorkspaceId(WORKSPACE_ID)).thenReturn(Optional.of(budget));

        WorkspaceFinopsAnomalyService service = createService(250);
        List<FinopsAnomalyResponse> anomalies = service.listCurrentWorkspace();

        assertThat(anomalies).anyMatch(a ->
                a.code().equals("budget_soft_limit_reached") && a.severity().equals("warning"));
        assertThat(anomalies).noneMatch(a -> a.code().equals("budget_hard_limit_reached"));
    }

    @Test
    @DisplayName("should not detect budget anomaly when budget is null")
    void shouldNotDetectBudgetAnomalyWhenNull() {
        setupHealthyDefaults();
        when(budgetRepository.findByWorkspaceId(WORKSPACE_ID)).thenReturn(Optional.empty());

        WorkspaceFinopsAnomalyService service = createService(500);
        List<FinopsAnomalyResponse> anomalies = service.listCurrentWorkspace();

        assertThat(anomalies).noneMatch(a ->
                a.code().equals("budget_hard_limit_reached") || a.code().equals("budget_soft_limit_reached"));
    }

    @Test
    @DisplayName("should detect critical success rate when rate < 70%")
    void shouldDetectCriticalSuccessRate() {
        setupHealthyDefaults();
        when(costLedgerRepository.countByWorkspaceId(WORKSPACE_ID)).thenReturn(10L);
        when(costLedgerRepository.countByWorkspaceIdAndStatus(WORKSPACE_ID, "completed")).thenReturn(5L);

        WorkspaceFinopsAnomalyService service = createService(0);
        List<FinopsAnomalyResponse> anomalies = service.listCurrentWorkspace();

        assertThat(anomalies).anyMatch(a ->
                a.code().equals("inference_success_rate_critical") && a.severity().equals("critical"));
    }

    @Test
    @DisplayName("should detect warning success rate when rate between 70-85%")
    void shouldDetectWarningSuccessRate() {
        setupHealthyDefaults();
        when(costLedgerRepository.countByWorkspaceId(WORKSPACE_ID)).thenReturn(10L);
        when(costLedgerRepository.countByWorkspaceIdAndStatus(WORKSPACE_ID, "completed")).thenReturn(8L);

        WorkspaceFinopsAnomalyService service = createService(0);
        List<FinopsAnomalyResponse> anomalies = service.listCurrentWorkspace();

        assertThat(anomalies).anyMatch(a ->
                a.code().equals("inference_success_rate_warning") && a.severity().equals("warning"));
        assertThat(anomalies).noneMatch(a -> a.code().equals("inference_success_rate_critical"));
    }

    @Test
    @DisplayName("should skip success rate check when total runs < 5")
    void shouldSkipSuccessRateUnder5Runs() {
        setupHealthyDefaults();
        when(costLedgerRepository.countByWorkspaceId(WORKSPACE_ID)).thenReturn(4L);
        when(costLedgerRepository.countByWorkspaceIdAndStatus(WORKSPACE_ID, "completed")).thenReturn(0L);

        WorkspaceFinopsAnomalyService service = createService(0);
        List<FinopsAnomalyResponse> anomalies = service.listCurrentWorkspace();

        assertThat(anomalies).noneMatch(a ->
                a.code().equals("inference_success_rate_critical") || a.code().equals("inference_success_rate_warning"));
    }

    @Test
    @DisplayName("should detect high average cost per run")
    void shouldDetectHighAverageCost() {
        setupHealthyDefaults();
        when(costLedgerRepository.countByWorkspaceId(WORKSPACE_ID)).thenReturn(10L);
        when(costLedgerRepository.countByWorkspaceIdAndStatus(WORKSPACE_ID, "completed")).thenReturn(10L);
        when(costLedgerRepository.averageEstimatedCostByWorkspaceId(WORKSPACE_ID)).thenReturn(0.12);

        WorkspaceFinopsAnomalyService service = createService(0);
        List<FinopsAnomalyResponse> anomalies = service.listCurrentWorkspace();

        assertThat(anomalies).anyMatch(a ->
                a.code().equals("average_cost_per_run_high") && a.severity().equals("warning"));
    }

    @Test
    @DisplayName("should skip cost anomaly when average is null")
    void shouldSkipCostWhenAverageNull() {
        setupHealthyDefaults();
        when(costLedgerRepository.countByWorkspaceId(WORKSPACE_ID)).thenReturn(10L);
        when(costLedgerRepository.countByWorkspaceIdAndStatus(WORKSPACE_ID, "completed")).thenReturn(10L);
        when(costLedgerRepository.averageEstimatedCostByWorkspaceId(WORKSPACE_ID)).thenReturn(null);

        WorkspaceFinopsAnomalyService service = createService(0);
        List<FinopsAnomalyResponse> anomalies = service.listCurrentWorkspace();

        assertThat(anomalies).noneMatch(a -> a.code().equals("average_cost_per_run_high"));
    }

    @Test
    @DisplayName("should detect negative credit balance as critical")
    void shouldDetectNegativeCreditBalance() {
        setupHealthyDefaults();
        when(creditLedgerRepository.sumCreditsByWorkspaceId(WORKSPACE_ID)).thenReturn(-5);

        WorkspaceFinopsAnomalyService service = createService(0);
        List<FinopsAnomalyResponse> anomalies = service.listCurrentWorkspace();

        assertThat(anomalies).anyMatch(a ->
                a.code().equals("credit_ledger_negative_balance") && a.severity().equals("critical"));
    }

    @Test
    @DisplayName("should detect zero credit balance as warning")
    void shouldDetectZeroCreditBalance() {
        setupHealthyDefaults();
        when(creditLedgerRepository.sumCreditsByWorkspaceId(WORKSPACE_ID)).thenReturn(0);

        WorkspaceFinopsAnomalyService service = createService(0);
        List<FinopsAnomalyResponse> anomalies = service.listCurrentWorkspace();

        assertThat(anomalies).anyMatch(a ->
                a.code().equals("credit_ledger_zero_balance") && a.severity().equals("warning"));
        assertThat(anomalies).noneMatch(a -> a.code().equals("credit_ledger_negative_balance"));
    }

    @Test
    @DisplayName("should detect pending payment events")
    void shouldDetectPendingPayments() {
        setupHealthyDefaults();
        when(paymentEventRepository.countByWorkspaceIdAndStatusAndProcessedAtIsNull(WORKSPACE_ID, "pending")).thenReturn(3L);

        WorkspaceFinopsAnomalyService service = createService(0);
        List<FinopsAnomalyResponse> anomalies = service.listCurrentWorkspace();

        assertThat(anomalies).anyMatch(a ->
                a.code().equals("payment_events_pending") && a.severity().equals("warning"));
    }

    @Test
    @DisplayName("should detect orphan payment events")
    void shouldDetectOrphanPayments() {
        setupHealthyDefaults();
        when(paymentEventRepository.countByWorkspaceIdAndInvoiceIsNull(WORKSPACE_ID)).thenReturn(2L);

        WorkspaceFinopsAnomalyService service = createService(0);
        List<FinopsAnomalyResponse> anomalies = service.listCurrentWorkspace();

        assertThat(anomalies).anyMatch(a ->
                a.code().equals("payment_events_orphan") && a.severity().equals("warning"));
    }

    @Test
    @DisplayName("should detect missing subscription")
    void shouldDetectMissingSubscription() {
        setupHealthyDefaults();
        when(subscriptionRepository.findByWorkspaceId(WORKSPACE_ID)).thenReturn(Optional.empty());

        WorkspaceFinopsAnomalyService service = createService(0);
        List<FinopsAnomalyResponse> anomalies = service.listCurrentWorkspace();

        assertThat(anomalies).anyMatch(a ->
                a.code().equals("missing_subscription") && a.severity().equals("warning"));
    }

    @Test
    @DisplayName("should return empty list when all checks are healthy")
    void shouldReturnEmptyWhenHealthy() {
        setupHealthyDefaults();

        WorkspaceFinopsAnomalyService service = createService(0);
        List<FinopsAnomalyResponse> anomalies = service.listCurrentWorkspace();

        assertThat(anomalies).isEmpty();
    }

    @Test
    @DisplayName("should return multiple anomalies when several conditions are met")
    void shouldReturnMultipleAnomalies() {
        setupHealthyDefaults();

        // Budget hard limit
        WorkspaceBudgetJpaEntity budget = new WorkspaceBudgetJpaEntity();
        budget.setWorkspaceId(WORKSPACE_ID);
        budget.setSoftLimitCredits(100);
        budget.setHardLimitCredits(200);
        when(budgetRepository.findByWorkspaceId(WORKSPACE_ID)).thenReturn(Optional.of(budget));

        // Negative credit balance
        when(creditLedgerRepository.sumCreditsByWorkspaceId(WORKSPACE_ID)).thenReturn(-10);

        // Missing subscription
        when(subscriptionRepository.findByWorkspaceId(WORKSPACE_ID)).thenReturn(Optional.empty());

        WorkspaceFinopsAnomalyService service = createService(250);
        List<FinopsAnomalyResponse> anomalies = service.listCurrentWorkspace();

        assertThat(anomalies).hasSizeGreaterThanOrEqualTo(3);
        assertThat(anomalies).anyMatch(a -> a.code().equals("budget_hard_limit_reached"));
        assertThat(anomalies).anyMatch(a -> a.code().equals("credit_ledger_negative_balance"));
        assertThat(anomalies).anyMatch(a -> a.code().equals("missing_subscription"));
    }

    @Test
    @DisplayName("should respect custom properties thresholds")
    void shouldRespectCustomProperties() {
        setupHealthyDefaults();
        when(costLedgerRepository.countByWorkspaceId(WORKSPACE_ID)).thenReturn(10L);
        when(costLedgerRepository.countByWorkspaceIdAndStatus(WORKSPACE_ID, "completed")).thenReturn(8L);
        when(costLedgerRepository.averageEstimatedCostByWorkspaceId(WORKSPACE_ID)).thenReturn(0.05);

        // With default properties (successRateWarning=85, costThreshold=0.08):
        // 80% success rate triggers warning, 0.05 avg cost is below 0.08 threshold
        FinopsAnomalyProperties defaultProps = defaultProperties();
        WorkspaceFinopsAnomalyService defaultService = createService(0, defaultProps);
        List<FinopsAnomalyResponse> defaultAnomalies = defaultService.listCurrentWorkspace();
        assertThat(defaultAnomalies).anyMatch(a -> a.code().equals("inference_success_rate_warning"));
        assertThat(defaultAnomalies).noneMatch(a -> a.code().equals("average_cost_per_run_high"));

        // With custom properties: raise warning threshold so 80% is OK, lower cost threshold so 0.05 triggers
        FinopsAnomalyProperties customProps = new FinopsAnomalyProperties();
        customProps.setSuccessRateCritical(50.0);
        customProps.setSuccessRateWarning(70.0);
        customProps.setCostThreshold(0.03);

        WorkspaceFinopsAnomalyService customService = createService(0, customProps);
        List<FinopsAnomalyResponse> customAnomalies = customService.listCurrentWorkspace();
        assertThat(customAnomalies).noneMatch(a -> a.code().equals("inference_success_rate_warning"));
        assertThat(customAnomalies).noneMatch(a -> a.code().equals("inference_success_rate_critical"));
        assertThat(customAnomalies).anyMatch(a -> a.code().equals("average_cost_per_run_high"));
    }

    // ---- Stubs ----

    private static final class StubWorkspaceMeteringService extends WorkspaceMeteringService {
        private final UsageMeteringSnapshot snapshot;

        private StubWorkspaceMeteringService(int consumedCredits) {
            super(null, null, null, null);
            this.snapshot = new UsageMeteringSnapshot(300, consumedCredits, Math.max(0, 300 - consumedCredits), 4, 1, 2);
        }

        @Override
        public UsageMeteringSnapshot currentSnapshot() {
            return snapshot;
        }
    }

    private static final class StubWorkspaceContextService extends WorkspaceContextService {
        private StubWorkspaceContextService() {
            super(null, null, null, null, null, null, new ObjectProvider<>() {
                @Override
                public HttpServletRequest getObject(Object... args) {
                    return null;
                }

                @Override
                public HttpServletRequest getIfAvailable() {
                    return null;
                }

                @Override
                public HttpServletRequest getIfUnique() {
                    return null;
                }

                @Override
                public HttpServletRequest getObject() {
                    return null;
                }
            });
        }

        @Override
        public void requirePermission(String permission) {
        }

        @Override
        public Long getWorkspaceId() {
            return WORKSPACE_ID;
        }

        @Override
        public Long getOrganizationId() {
            return 1L;
        }

        @Override
        public Long getActorUserIdOrNull() {
            return 1L;
        }
    }
}
