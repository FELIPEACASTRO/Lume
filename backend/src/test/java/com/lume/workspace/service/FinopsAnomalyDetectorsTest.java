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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Finops Anomaly Detectors - Unit Tests")
class FinopsAnomalyDetectorsTest {

    private static final Long WORKSPACE_ID = 1L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 12, 10, 0, 0);

    // ── Budget Limit ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("BudgetLimitAnomalyDetector")
    class BudgetLimitTests {

        private WorkspaceBudgetJpaRepository budgetRepository;
        private BudgetLimitAnomalyDetector detector;

        @BeforeEach
        void setUp() {
            budgetRepository = mock(WorkspaceBudgetJpaRepository.class);
            var meteringService = new StubWorkspaceMeteringService(0);
            detector = new BudgetLimitAnomalyDetector(budgetRepository, meteringService);
        }

        @Test
        @DisplayName("returns critical anomaly when consumed credits reach hard limit")
        void shouldDetectHardLimitReached() {
            WorkspaceBudgetJpaEntity budget = new WorkspaceBudgetJpaEntity();
            budget.setWorkspaceId(WORKSPACE_ID);
            budget.setSoftLimitCredits(200);
            budget.setHardLimitCredits(400);
            when(budgetRepository.findByWorkspaceId(WORKSPACE_ID)).thenReturn(Optional.of(budget));

            var meteringService = new StubWorkspaceMeteringService(400);
            var localDetector = new BudgetLimitAnomalyDetector(budgetRepository, meteringService);

            List<FinopsAnomalyResponse> anomalies = localDetector.detect(WORKSPACE_ID, NOW);

            assertThat(anomalies).hasSize(1);
            assertThat(anomalies.get(0).code()).isEqualTo("budget_hard_limit_reached");
            assertThat(anomalies.get(0).severity()).isEqualTo("critical");
            assertThat(anomalies.get(0).status()).isEqualTo("open");
        }

        @Test
        @DisplayName("returns no anomaly when consumed credits are below soft limit")
        void shouldReturnEmptyWhenBelowSoftLimit() {
            WorkspaceBudgetJpaEntity budget = new WorkspaceBudgetJpaEntity();
            budget.setWorkspaceId(WORKSPACE_ID);
            budget.setSoftLimitCredits(200);
            budget.setHardLimitCredits(400);
            when(budgetRepository.findByWorkspaceId(WORKSPACE_ID)).thenReturn(Optional.of(budget));

            var meteringService = new StubWorkspaceMeteringService(100);
            var localDetector = new BudgetLimitAnomalyDetector(budgetRepository, meteringService);

            List<FinopsAnomalyResponse> anomalies = localDetector.detect(WORKSPACE_ID, NOW);

            assertThat(anomalies).isEmpty();
        }
    }

    // ── Cost Per Run ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("CostPerRunAnomalyDetector")
    class CostPerRunTests {

        private WorkspaceCostLedgerEntryJpaRepository costLedgerRepository;
        private FinopsAnomalyProperties properties;
        private CostPerRunAnomalyDetector detector;

        @BeforeEach
        void setUp() {
            costLedgerRepository = mock(WorkspaceCostLedgerEntryJpaRepository.class);
            properties = new FinopsAnomalyProperties();
            properties.setCostThreshold(0.08);
            detector = new CostPerRunAnomalyDetector(costLedgerRepository, properties);
        }

        @Test
        @DisplayName("returns warning when average cost per run exceeds threshold")
        void shouldDetectHighAverageCost() {
            when(costLedgerRepository.countByWorkspaceId(WORKSPACE_ID)).thenReturn(10L);
            when(costLedgerRepository.averageEstimatedCostByWorkspaceId(WORKSPACE_ID)).thenReturn(0.15);

            List<FinopsAnomalyResponse> anomalies = detector.detect(WORKSPACE_ID, NOW);

            assertThat(anomalies).hasSize(1);
            assertThat(anomalies.get(0).code()).isEqualTo("average_cost_per_run_high");
            assertThat(anomalies.get(0).severity()).isEqualTo("warning");
        }

        @Test
        @DisplayName("returns no anomaly when average cost is below threshold")
        void shouldReturnEmptyWhenCostNormal() {
            when(costLedgerRepository.countByWorkspaceId(WORKSPACE_ID)).thenReturn(10L);
            when(costLedgerRepository.averageEstimatedCostByWorkspaceId(WORKSPACE_ID)).thenReturn(0.03);

            List<FinopsAnomalyResponse> anomalies = detector.detect(WORKSPACE_ID, NOW);

            assertThat(anomalies).isEmpty();
        }
    }

    // ── Credit Balance ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("CreditBalanceAnomalyDetector")
    class CreditBalanceTests {

        private WorkspaceCreditLedgerEntryJpaRepository creditLedgerRepository;
        private CreditBalanceAnomalyDetector detector;

        @BeforeEach
        void setUp() {
            creditLedgerRepository = mock(WorkspaceCreditLedgerEntryJpaRepository.class);
            detector = new CreditBalanceAnomalyDetector(creditLedgerRepository);
        }

        @Test
        @DisplayName("returns critical anomaly when credit balance is negative")
        void shouldDetectNegativeBalance() {
            when(creditLedgerRepository.sumCreditsByWorkspaceId(WORKSPACE_ID)).thenReturn(-50);

            List<FinopsAnomalyResponse> anomalies = detector.detect(WORKSPACE_ID, NOW);

            assertThat(anomalies).hasSize(1);
            assertThat(anomalies.get(0).code()).isEqualTo("credit_ledger_negative_balance");
            assertThat(anomalies.get(0).severity()).isEqualTo("critical");
        }

        @Test
        @DisplayName("returns no anomaly when credit balance is positive")
        void shouldReturnEmptyWhenBalanceHealthy() {
            when(creditLedgerRepository.sumCreditsByWorkspaceId(WORKSPACE_ID)).thenReturn(250);

            List<FinopsAnomalyResponse> anomalies = detector.detect(WORKSPACE_ID, NOW);

            assertThat(anomalies).isEmpty();
        }
    }

    // ── Inference Success Rate ──────────────────────────────────────────────

    @Nested
    @DisplayName("InferenceSuccessRateAnomalyDetector")
    class InferenceSuccessRateTests {

        private WorkspaceCostLedgerEntryJpaRepository costLedgerRepository;
        private FinopsAnomalyProperties properties;
        private InferenceSuccessRateAnomalyDetector detector;

        @BeforeEach
        void setUp() {
            costLedgerRepository = mock(WorkspaceCostLedgerEntryJpaRepository.class);
            properties = new FinopsAnomalyProperties();
            properties.setSuccessRateCritical(70.0);
            properties.setSuccessRateWarning(85.0);
            detector = new InferenceSuccessRateAnomalyDetector(costLedgerRepository, properties);
        }

        @Test
        @DisplayName("returns critical anomaly when success rate is below critical threshold")
        void shouldDetectCriticalSuccessRate() {
            when(costLedgerRepository.countByWorkspaceId(WORKSPACE_ID)).thenReturn(20L);
            when(costLedgerRepository.countByWorkspaceIdAndStatus(WORKSPACE_ID, "completed")).thenReturn(10L);

            List<FinopsAnomalyResponse> anomalies = detector.detect(WORKSPACE_ID, NOW);

            assertThat(anomalies).hasSize(1);
            assertThat(anomalies.get(0).code()).isEqualTo("inference_success_rate_critical");
            assertThat(anomalies.get(0).severity()).isEqualTo("critical");
        }

        @Test
        @DisplayName("returns no anomaly when success rate is above warning threshold")
        void shouldReturnEmptyWhenSuccessRateHigh() {
            when(costLedgerRepository.countByWorkspaceId(WORKSPACE_ID)).thenReturn(20L);
            when(costLedgerRepository.countByWorkspaceIdAndStatus(WORKSPACE_ID, "completed")).thenReturn(19L);

            List<FinopsAnomalyResponse> anomalies = detector.detect(WORKSPACE_ID, NOW);

            assertThat(anomalies).isEmpty();
        }
    }

    // ── Payment Events ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("PaymentEventsAnomalyDetector")
    class PaymentEventsTests {

        private WorkspacePaymentEventJpaRepository paymentEventRepository;
        private PaymentEventsAnomalyDetector detector;

        @BeforeEach
        void setUp() {
            paymentEventRepository = mock(WorkspacePaymentEventJpaRepository.class);
            detector = new PaymentEventsAnomalyDetector(paymentEventRepository);
        }

        @Test
        @DisplayName("returns anomalies when pending and orphan payment events exist")
        void shouldDetectPendingAndOrphanEvents() {
            when(paymentEventRepository.countByWorkspaceIdAndStatusAndProcessedAtIsNull(WORKSPACE_ID, "pending")).thenReturn(3L);
            when(paymentEventRepository.countByWorkspaceIdAndInvoiceIsNull(WORKSPACE_ID)).thenReturn(2L);

            List<FinopsAnomalyResponse> anomalies = detector.detect(WORKSPACE_ID, NOW);

            assertThat(anomalies).hasSize(2);
            assertThat(anomalies).extracting(FinopsAnomalyResponse::code)
                    .containsExactly("payment_events_pending", "payment_events_orphan");
            assertThat(anomalies).allMatch(a -> "warning".equals(a.severity()));
        }

        @Test
        @DisplayName("returns no anomaly when no pending or orphan events exist")
        void shouldReturnEmptyWhenEventsNormal() {
            when(paymentEventRepository.countByWorkspaceIdAndStatusAndProcessedAtIsNull(WORKSPACE_ID, "pending")).thenReturn(0L);
            when(paymentEventRepository.countByWorkspaceIdAndInvoiceIsNull(WORKSPACE_ID)).thenReturn(0L);

            List<FinopsAnomalyResponse> anomalies = detector.detect(WORKSPACE_ID, NOW);

            assertThat(anomalies).isEmpty();
        }
    }

    // ── Subscription ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("SubscriptionAnomalyDetector")
    class SubscriptionTests {

        private WorkspaceSubscriptionJpaRepository subscriptionRepository;
        private SubscriptionAnomalyDetector detector;

        @BeforeEach
        void setUp() {
            subscriptionRepository = mock(WorkspaceSubscriptionJpaRepository.class);
            detector = new SubscriptionAnomalyDetector(subscriptionRepository);
        }

        @Test
        @DisplayName("returns warning when workspace has no subscription")
        void shouldDetectMissingSubscription() {
            when(subscriptionRepository.findByWorkspaceId(WORKSPACE_ID)).thenReturn(Optional.empty());

            List<FinopsAnomalyResponse> anomalies = detector.detect(WORKSPACE_ID, NOW);

            assertThat(anomalies).hasSize(1);
            assertThat(anomalies.get(0).code()).isEqualTo("missing_subscription");
            assertThat(anomalies.get(0).severity()).isEqualTo("warning");
        }

        @Test
        @DisplayName("returns no anomaly when workspace has active subscription")
        void shouldReturnEmptyWhenSubscriptionActive() {
            WorkspaceSubscriptionJpaEntity sub = new WorkspaceSubscriptionJpaEntity();
            sub.setWorkspaceId(WORKSPACE_ID);
            sub.setSubscriptionStatus("active");
            when(subscriptionRepository.findByWorkspaceId(WORKSPACE_ID)).thenReturn(Optional.of(sub));

            List<FinopsAnomalyResponse> anomalies = detector.detect(WORKSPACE_ID, NOW);

            assertThat(anomalies).isEmpty();
        }
    }

    // ── test doubles ────────────────────────────────────────────────────────

    /**
     * Stub for WorkspaceMeteringService that returns a fixed consumed-credits value.
     * We cannot mock this concrete class with Mockito on Java 24, so we override
     * the method directly.
     */
    private static final class StubWorkspaceMeteringService extends WorkspaceMeteringService {

        private final int consumedCredits;

        StubWorkspaceMeteringService(int consumedCredits) {
            super(null, null, null, null);
            this.consumedCredits = consumedCredits;
        }

        @Override
        public UsageMeteringSnapshot currentSnapshot() {
            return new UsageMeteringSnapshot(300, consumedCredits, 300 - consumedCredits, 0, 0, 0);
        }
    }
}
