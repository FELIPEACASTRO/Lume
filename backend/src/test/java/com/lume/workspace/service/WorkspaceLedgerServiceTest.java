package com.lume.workspace.service;

import com.lume.infrastructure.config.SlaProperties;
import com.lume.workspace.dto.ProviderStatusResponse;
import com.lume.workspace.entity.WorkspaceCostLedgerEntryJpaEntity;
import com.lume.workspace.entity.WorkspaceCreditLedgerEntryJpaEntity;
import com.lume.workspace.entity.WorkspaceUsageEventJpaEntity;
import com.lume.workspace.inference.security.EnvironmentSecretResolver;
import com.lume.workspace.repository.SupportTicketJpaRepository;
import com.lume.workspace.repository.TaskJpaRepository;
import com.lume.workspace.repository.WorkspaceByokConnectionJpaRepository;
import com.lume.workspace.repository.WorkspaceCostLedgerEntryJpaRepository;
import com.lume.workspace.repository.WorkspaceCreditLedgerEntryJpaRepository;
import com.lume.workspace.repository.WorkspaceFinopsReconciliationRunJpaRepository;
import com.lume.workspace.repository.WorkspaceInvoiceJpaRepository;
import com.lume.workspace.repository.WorkspaceJpaRepository;
import com.lume.workspace.repository.WorkspacePaymentEventJpaRepository;
import com.lume.workspace.repository.WorkspaceUsageEventJpaRepository;
import com.lume.workspace.entity.SupportTicketJpaEntity;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.env.MockEnvironment;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("WorkspaceLedgerService - Unit Tests")
class WorkspaceLedgerServiceTest {

    private WorkspaceUsageEventJpaRepository usageEventRepository;
    private WorkspaceCreditLedgerEntryJpaRepository creditLedgerRepository;
    private WorkspaceCostLedgerEntryJpaRepository costLedgerRepository;
    private WorkspaceJpaRepository workspaceRepository;
    private TaskJpaRepository taskRepository;
    private WorkspaceInvoiceJpaRepository invoiceRepository;
    private WorkspacePaymentEventJpaRepository paymentEventRepository;
    private WorkspaceFinopsReconciliationRunJpaRepository reconciliationRunRepository;
    private SupportTicketJpaRepository supportTicketRepository;
    private WorkspaceByokConnectionJpaRepository byokConnectionRepository;
    private StubProviderCatalogService providerCatalogService;
    private WorkspaceLedgerService service;

    @BeforeEach
    void setUp() {
        usageEventRepository = mock(WorkspaceUsageEventJpaRepository.class);
        creditLedgerRepository = mock(WorkspaceCreditLedgerEntryJpaRepository.class);
        costLedgerRepository = mock(WorkspaceCostLedgerEntryJpaRepository.class);
        workspaceRepository = mock(WorkspaceJpaRepository.class);
        taskRepository = mock(TaskJpaRepository.class);
        invoiceRepository = mock(WorkspaceInvoiceJpaRepository.class);
        paymentEventRepository = mock(WorkspacePaymentEventJpaRepository.class);
        reconciliationRunRepository = mock(WorkspaceFinopsReconciliationRunJpaRepository.class);
        supportTicketRepository = mock(SupportTicketJpaRepository.class);
        byokConnectionRepository = mock(WorkspaceByokConnectionJpaRepository.class);
        providerCatalogService = new StubProviderCatalogService();

        when(usageEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(creditLedgerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(costLedgerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service = new WorkspaceLedgerService(
                usageEventRepository,
                creditLedgerRepository,
                costLedgerRepository,
                workspaceRepository,
                taskRepository,
                invoiceRepository,
                paymentEventRepository,
                reconciliationRunRepository,
                supportTicketRepository,
                byokConnectionRepository,
                providerCatalogService,
                new StubWorkspaceContextService(),
                new StubSupportTicketSlaService()
        );
    }

    @Test
    @DisplayName("recordUsageEventForWorkspace persists event with correct fields")
    void shouldRecordUsageEvent() {
        service.recordUsageEventForWorkspace(1L, 10L, "inference.completed", "agent_thread", "thread-1", "Completed.");

        verify(usageEventRepository).save(any(WorkspaceUsageEventJpaEntity.class));
    }

    @Test
    @DisplayName("recordCreditEntry computes balanceAfter correctly")
    void shouldRecordCreditEntryWithBalance() {
        when(creditLedgerRepository.sumCreditsByWorkspaceId(1L)).thenReturn(500);

        service.recordCreditEntry(1L, "credit", "subscription_onboarding", "sub-1", 100, "Bonus");

        verify(creditLedgerRepository).save(any(WorkspaceCreditLedgerEntryJpaEntity.class));
    }

    @Test
    @DisplayName("recordCostEntry persists cost with all fields")
    void shouldRecordCostEntry() {
        var record = new WorkspaceLedgerService.CostLedgerRecord(
                1L, "openai", "gpt-4", "text", "req-1",
                "completed", 150, 200, 0.0035, 1200L,
                false, "quality", "routed to openai"
        );

        service.recordCostEntry(record);

        verify(costLedgerRepository).save(any(WorkspaceCostLedgerEntryJpaEntity.class));
    }

    @Test
    @DisplayName("listUsageEvents returns mapped responses")
    void shouldListUsageEvents() {
        WorkspaceUsageEventJpaEntity event = new WorkspaceUsageEventJpaEntity();
        event.setWorkspaceId(1L);
        event.setEventType("agents.thread_created");
        event.setResourceType("agent_thread");
        event.setResourceId("t-1");
        event.setActorUserId(10L);
        event.setDetails("Thread criada.");
        triggerPrePersist(event);

        when(usageEventRepository.findByWorkspaceIdOrderByCreatedAtDesc(eq(1L), any(PageRequest.class)))
                .thenReturn(List.of(event));

        var result = service.listUsageEvents(10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).eventType()).isEqualTo("agents.thread_created");
        assertThat(result.get(0).resourceType()).isEqualTo("agent_thread");
    }

    @Test
    @DisplayName("listCreditEntries returns mapped responses")
    void shouldListCreditEntries() {
        WorkspaceCreditLedgerEntryJpaEntity entry = new WorkspaceCreditLedgerEntryJpaEntity();
        entry.setWorkspaceId(1L);
        entry.setEntryType("credit");
        entry.setSourceType("subscription_onboarding");
        entry.setSourceId("sub-1");
        entry.setCreditsDelta(100);
        entry.setBalanceAfter(600);
        entry.setNote("Onboarding.");
        triggerPrePersist(entry);

        when(creditLedgerRepository.findByWorkspaceIdOrderByCreatedAtDesc(eq(1L), any(PageRequest.class)))
                .thenReturn(List.of(entry));

        var result = service.listCreditEntries(10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).entryType()).isEqualTo("credit");
        assertThat(result.get(0).creditsDelta()).isEqualTo(100);
        assertThat(result.get(0).balanceAfter()).isEqualTo(600);
    }

    @Test
    @DisplayName("scorecard aggregates data from all repositories")
    void shouldComputeScorecardCorrectly() {
        when(taskRepository.countByWorkspaceId(1L)).thenReturn(10L);
        when(taskRepository.countByWorkspaceIdAndRuntimeState(1L, "completed")).thenReturn(8L);
        when(costLedgerRepository.countByWorkspaceId(1L)).thenReturn(20L);
        when(costLedgerRepository.countByWorkspaceIdAndStatus(1L, "completed")).thenReturn(19L);
        when(costLedgerRepository.averageEstimatedCostByWorkspaceId(1L)).thenReturn(0.005);
        when(invoiceRepository.countByWorkspaceIdAndStatus(1L, "paid")).thenReturn(3L);
        when(paymentEventRepository.countByWorkspaceIdAndStatus(1L, "failed")).thenReturn(0L);
        when(paymentEventRepository.countByWorkspaceIdAndInvoiceIsNull(1L)).thenReturn(0L);
        when(paymentEventRepository.countByWorkspaceIdAndStatusAndProcessedAtIsNull(1L, "pending")).thenReturn(0L);
        when(reconciliationRunRepository.findFirstByWorkspaceIdOrderByExecutedAtDesc(1L)).thenReturn(java.util.Optional.empty());
        when(supportTicketRepository.countByWorkspaceIdAndStatusIn(eq(1L), any())).thenReturn(2L);
        when(supportTicketRepository.countByWorkspaceIdAndStatusInAndSeverity(eq(1L), any(), eq("critical"))).thenReturn(0L);
        when(supportTicketRepository.findByWorkspaceIdAndStatusIn(eq(1L), any())).thenReturn(List.of());
        when(byokConnectionRepository.countByWorkspaceId(1L)).thenReturn(1L);
        when(byokConnectionRepository.countByWorkspaceIdAndHealthStatus(1L, "healthy")).thenReturn(1L);
        when(usageEventRepository.countDistinctActorUserIdByWorkspaceIdAndActorUserIdIsNotNullAndCreatedAtAfter(eq(1L), any(LocalDateTime.class)))
                .thenReturn(5L);
        providerCatalogService.statuses = List.of();
        when(creditLedgerRepository.sumCreditsByWorkspaceId(1L)).thenReturn(450);
        when(workspaceRepository.findById(1L)).thenReturn(java.util.Optional.empty());
        when(taskRepository.findFirstByWorkspaceIdOrderByCreatedAtAsc(1L)).thenReturn(java.util.Optional.empty());

        var scorecard = service.scorecard();

        assertThat(scorecard.taskCompletionRate()).isEqualTo(80.0);
        assertThat(scorecard.inferenceSuccessRate()).isEqualTo(95.0);
        assertThat(scorecard.paidInvoicesCount()).isEqualTo(3);
        assertThat(scorecard.openSupportTickets()).isEqualTo(2);
        assertThat(scorecard.currentCreditBalance()).isEqualTo(450);
        assertThat(scorecard.byokConnections()).isEqualTo(1);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void triggerPrePersist(Object entity) {
        try {
            var method = entity.getClass().getDeclaredMethod("onCreate");
            method.setAccessible(true);
            method.invoke(entity);
        } catch (Exception ignored) {}
    }

    // ── test doubles ────────────────────────────────────────────────────────

    private static final class StubWorkspaceContextService extends WorkspaceContextService {
        private StubWorkspaceContextService() {
            super(null, null, null, null, null, null, new ObjectProvider<>() {
                @Override public HttpServletRequest getObject(Object... args) { return null; }
                @Override public HttpServletRequest getIfAvailable() { return null; }
                @Override public HttpServletRequest getIfUnique() { return null; }
                @Override public HttpServletRequest getObject() { return null; }
            });
        }

        @Override public void requirePermission(String permission) { }
        @Override public Long getWorkspaceId() { return 1L; }
        @Override public Long getActorUserIdOrNull() { return 1L; }
        @Override public String getActorName() { return "Lume Operator"; }
    }

    private static final class StubProviderCatalogService extends ProviderCatalogService {
        List<ProviderStatusResponse> statuses = List.of();

        private StubProviderCatalogService() {
            super(new EnvironmentSecretResolver(new MockEnvironment()));
        }

        @Override
        public List<ProviderStatusResponse> listProviderStatuses() {
            return statuses;
        }
    }

    private static final class StubSupportTicketSlaService extends SupportTicketSlaService {
        private StubSupportTicketSlaService() {
            super(new SlaProperties(), Clock.fixed(Instant.parse("2026-03-12T12:00:00Z"), ZoneId.of("UTC")));
        }

        @Override
        public boolean isBreached(SupportTicketJpaEntity ticket) {
            return false;
        }
    }
}
