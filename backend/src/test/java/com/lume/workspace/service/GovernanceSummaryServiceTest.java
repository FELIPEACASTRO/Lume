package com.lume.workspace.service;

import com.lume.infrastructure.config.SlaProperties;
import com.lume.workspace.dto.ProviderStatusResponse;
import com.lume.workspace.dto.SettingsGovernanceSummaryResponse;
import com.lume.workspace.entity.SupportTicketJpaEntity;
import com.lume.workspace.entity.WorkspaceFinopsReconciliationRunJpaEntity;
import com.lume.workspace.inference.security.EnvironmentSecretResolver;
import com.lume.workspace.repository.SupportTicketJpaRepository;
import com.lume.workspace.repository.WorkspaceByokConnectionJpaRepository;
import com.lume.workspace.repository.WorkspaceFinopsReconciliationRunJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GovernanceSummaryService - Unit Tests")
class GovernanceSummaryServiceTest {

    private SupportTicketJpaRepository supportTicketRepository;
    private WorkspaceByokConnectionJpaRepository byokConnectionRepository;
    private WorkspaceFinopsReconciliationRunJpaRepository reconciliationRunRepository;
    private StubProviderCatalogService stubProviderCatalogService;
    private StubSupportTicketSlaService stubSupportTicketSlaService;
    private GovernanceSummaryService service;

    @BeforeEach
    void setUp() {
        supportTicketRepository = mock(SupportTicketJpaRepository.class);
        byokConnectionRepository = mock(WorkspaceByokConnectionJpaRepository.class);
        reconciliationRunRepository = mock(WorkspaceFinopsReconciliationRunJpaRepository.class);
        stubProviderCatalogService = new StubProviderCatalogService();
        stubSupportTicketSlaService = new StubSupportTicketSlaService();

        service = new GovernanceSummaryService(
                supportTicketRepository,
                byokConnectionRepository,
                reconciliationRunRepository,
                stubProviderCatalogService,
                stubSupportTicketSlaService
        );
    }

    @Test
    @DisplayName("should build governance summary with all fields populated")
    void shouldBuildGovernanceSummary() {
        when(supportTicketRepository.countByWorkspaceIdAndStatusIn(eq(1L), anyList())).thenReturn(5L);
        when(supportTicketRepository.countByWorkspaceIdAndStatusInAndSeverity(eq(1L), anyList(), eq("critical"))).thenReturn(2L);
        when(supportTicketRepository.findByWorkspaceIdAndStatusIn(eq(1L), anyList())).thenReturn(List.of());
        when(byokConnectionRepository.countByWorkspaceId(1L)).thenReturn(3L);
        when(byokConnectionRepository.countByWorkspaceIdAndHealthStatus(1L, "healthy")).thenReturn(2L);

        WorkspaceFinopsReconciliationRunJpaEntity reconciliation = new WorkspaceFinopsReconciliationRunJpaEntity();
        reconciliation.setReconciliationStatus("balanced");
        reconciliation.setExecutedAt(LocalDateTime.of(2026, 3, 10, 14, 0));
        when(reconciliationRunRepository.findFirstByWorkspaceIdOrderByExecutedAtDesc(1L))
                .thenReturn(Optional.of(reconciliation));

        stubProviderCatalogService.statuses = List.of(
                providerStatus("core_live"),
                providerStatus("core_live"),
                providerStatus("supported_restricted"),
                providerStatus("blocked")
        );

        SettingsGovernanceSummaryResponse response = service.governanceSummary(1L);

        assertThat(response.openSupportTickets()).isEqualTo(5L);
        assertThat(response.criticalOpenSupportTickets()).isEqualTo(2L);
        assertThat(response.byokConnections()).isEqualTo(3L);
        assertThat(response.healthyByokConnections()).isEqualTo(2L);
        assertThat(response.coreLiveProviders()).isEqualTo(2);
        assertThat(response.supportedRestrictedProviders()).isEqualTo(1);
        assertThat(response.blockedProviders()).isEqualTo(1);
        assertThat(response.lastReconciliationStatus()).isEqualTo("balanced");
        assertThat(response.lastReconciliationExecutedAt()).isNotNull();
        assertThat(response.note()).contains("balanced");
    }

    @Test
    @DisplayName("should count overdue tickets via SLA service")
    void shouldCountOverdueTicketsViaSla() {
        SupportTicketJpaEntity ticket1 = supportTicket("t1", "open", "critical");
        SupportTicketJpaEntity ticket2 = supportTicket("t2", "in_progress", "medium");
        SupportTicketJpaEntity ticket3 = supportTicket("t3", "open", "low");

        when(supportTicketRepository.countByWorkspaceIdAndStatusIn(eq(1L), anyList())).thenReturn(3L);
        when(supportTicketRepository.countByWorkspaceIdAndStatusInAndSeverity(eq(1L), anyList(), eq("critical"))).thenReturn(1L);
        when(supportTicketRepository.findByWorkspaceIdAndStatusIn(eq(1L), anyList()))
                .thenReturn(List.of(ticket1, ticket2, ticket3));
        when(byokConnectionRepository.countByWorkspaceId(1L)).thenReturn(0L);
        when(byokConnectionRepository.countByWorkspaceIdAndHealthStatus(1L, "healthy")).thenReturn(0L);
        when(reconciliationRunRepository.findFirstByWorkspaceIdOrderByExecutedAtDesc(1L)).thenReturn(Optional.empty());

        stubSupportTicketSlaService.breachedTicketIds = Set.of("t1");

        SettingsGovernanceSummaryResponse response = service.governanceSummary(1L);

        assertThat(response.overdueSupportTickets()).isEqualTo(1L);
    }

    @Test
    @DisplayName("should count provider tier distribution correctly")
    void shouldCountProviderTierDistribution() {
        when(supportTicketRepository.countByWorkspaceIdAndStatusIn(eq(1L), anyList())).thenReturn(0L);
        when(supportTicketRepository.countByWorkspaceIdAndStatusInAndSeverity(eq(1L), anyList(), eq("critical"))).thenReturn(0L);
        when(supportTicketRepository.findByWorkspaceIdAndStatusIn(eq(1L), anyList())).thenReturn(List.of());
        when(byokConnectionRepository.countByWorkspaceId(1L)).thenReturn(0L);
        when(byokConnectionRepository.countByWorkspaceIdAndHealthStatus(1L, "healthy")).thenReturn(0L);
        when(reconciliationRunRepository.findFirstByWorkspaceIdOrderByExecutedAtDesc(1L)).thenReturn(Optional.empty());

        stubProviderCatalogService.statuses = List.of(
                providerStatus("core_live"),
                providerStatus("core_live"),
                providerStatus("core_live"),
                providerStatus("supported_restricted"),
                providerStatus("supported_restricted"),
                providerStatus("blocked")
        );

        SettingsGovernanceSummaryResponse response = service.governanceSummary(1L);

        assertThat(response.coreLiveProviders()).isEqualTo(3);
        assertThat(response.supportedRestrictedProviders()).isEqualTo(2);
        assertThat(response.blockedProviders()).isEqualTo(1);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private SupportTicketJpaEntity supportTicket(String id, String status, String severity) {
        SupportTicketJpaEntity ticket = new SupportTicketJpaEntity();
        ticket.setId(id);
        ticket.setWorkspaceId(1L);
        ticket.setStatus(status);
        ticket.setSeverity(severity);
        ticket.setTitle("Test ticket " + id);
        ticket.setDescription("Test description");
        ticket.setCategory("general");
        return ticket;
    }

    private ProviderStatusResponse providerStatus(String tier) {
        return new ProviderStatusResponse(
                "provider-" + tier,
                "Provider " + tier,
                true,
                true,
                "implemented",
                "production",
                "enabled",
                "general",
                false,
                "sse",
                tier,
                "stable",
                "ready",
                "pending_live_smoke",
                null,
                null,
                List.of()
        );
    }

    // ── test doubles ─────────────────────────────────────────────────────────

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

        Set<String> breachedTicketIds = Set.of();

        private StubSupportTicketSlaService() {
            super(new SlaProperties(), Clock.fixed(Instant.parse("2026-03-12T12:00:00Z"), ZoneId.of("UTC")));
        }

        @Override
        public boolean isBreached(SupportTicketJpaEntity ticket) {
            return breachedTicketIds.contains(ticket.getId());
        }
    }
}
