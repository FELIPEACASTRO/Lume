package com.lume.workspace.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lume.domain.exception.BusinessRuleException;
import com.lume.domain.exception.ResourceNotFoundException;
import com.lume.workspace.dto.CreateSupportTicketRequest;
import com.lume.workspace.dto.SupportTicketResponse;
import com.lume.workspace.dto.UpdateSupportTicketRequest;
import com.lume.workspace.entity.SupportTicketJpaEntity;
import com.lume.workspace.repository.SupportTicketJpaRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("SupportTicketService - Unit Tests")
class SupportTicketServiceTest {

    private SupportTicketJpaRepository repository;
    private SpyWorkspaceLedgerService ledgerService;
    private SpyAuditLogService auditLogService;
    private SupportTicketService service;

    @BeforeEach
    void setUp() {
        repository = mock(SupportTicketJpaRepository.class);
        ledgerService = new SpyWorkspaceLedgerService();
        auditLogService = new SpyAuditLogService();

        when(repository.save(any(SupportTicketJpaEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service = new SupportTicketService(
                repository,
                new StubWorkspaceContextService(),
                ledgerService,
                auditLogService,
                new NoOpSupportTicketSlaService()
        );
    }

    @Test
    @DisplayName("shouldCreateWithNormalizedCategoryAndSeverity")
    void shouldCreateWithNormalizedCategoryAndSeverity() {
        SupportTicketResponse response = service.createCurrentWorkspace(
                new CreateSupportTicketRequest("My Title", "Some description", "Provider", "HIGH")
        );

        assertThat(response.category()).isEqualTo("provider");
        assertThat(response.severity()).isEqualTo("high");
    }

    @Test
    @DisplayName("shouldGenerateTicketIdWithUuidPrefix")
    void shouldGenerateTicketIdWithUuidPrefix() {
        SupportTicketResponse response = service.createCurrentWorkspace(
                new CreateSupportTicketRequest("Title", "Desc", "billing", "low")
        );

        assertThat(response.id()).startsWith("ticket-");
        assertThat(response.id()).hasSizeGreaterThanOrEqualTo(43);
    }

    @Test
    @DisplayName("shouldDefaultCategoryToOperational")
    void shouldDefaultCategoryToOperational() {
        SupportTicketResponse response = service.createCurrentWorkspace(
                new CreateSupportTicketRequest("Title", "Desc", "", "low")
        );

        assertThat(response.category()).isEqualTo("operational");
    }

    @Test
    @DisplayName("shouldDefaultSeverityToMedium")
    void shouldDefaultSeverityToMedium() {
        SupportTicketResponse response = service.createCurrentWorkspace(
                new CreateSupportTicketRequest("Title", "Desc", "billing", "")
        );

        assertThat(response.severity()).isEqualTo("medium");
    }

    @Test
    @DisplayName("shouldRejectInvalidCategory")
    void shouldRejectInvalidCategory() {
        assertThatThrownBy(() -> service.createCurrentWorkspace(
                new CreateSupportTicketRequest("Title", "Desc", "invalid", "low")
        )).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("shouldRejectInvalidSeverity")
    void shouldRejectInvalidSeverity() {
        assertThatThrownBy(() -> service.createCurrentWorkspace(
                new CreateSupportTicketRequest("Title", "Desc", "billing", "extreme")
        )).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("shouldUpdateStatusWithNormalization")
    void shouldUpdateStatusWithNormalization() {
        SupportTicketJpaEntity existing = buildExistingTicket();
        when(repository.findByIdAndWorkspaceId("ticket-123", 1L)).thenReturn(Optional.of(existing));

        SupportTicketResponse response = service.updateCurrentWorkspace(
                "ticket-123",
                new UpdateSupportTicketRequest("In Progress", null, null, null)
        );

        assertThat(response.status()).isEqualTo("in_progress");
    }

    @Test
    @DisplayName("shouldRejectInvalidStatus")
    void shouldRejectInvalidStatus() {
        SupportTicketJpaEntity existing = buildExistingTicket();
        when(repository.findByIdAndWorkspaceId("ticket-123", 1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.updateCurrentWorkspace(
                "ticket-123",
                new UpdateSupportTicketRequest("cancelled", null, null, null)
        )).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("shouldClearBlankResolutionNote")
    void shouldClearBlankResolutionNote() {
        SupportTicketJpaEntity existing = buildExistingTicket();
        existing.setResolutionNote("Old note");
        when(repository.findByIdAndWorkspaceId("ticket-123", 1L)).thenReturn(Optional.of(existing));

        SupportTicketResponse response = service.updateCurrentWorkspace(
                "ticket-123",
                new UpdateSupportTicketRequest(null, " ", null, null)
        );

        assertThat(response.resolutionNote()).isNull();
    }

    @Test
    @DisplayName("shouldPreserveFieldsOnPartialUpdate")
    void shouldPreserveFieldsOnPartialUpdate() {
        SupportTicketJpaEntity existing = buildExistingTicket();
        existing.setCategory("billing");
        existing.setSeverity("high");
        when(repository.findByIdAndWorkspaceId("ticket-123", 1L)).thenReturn(Optional.of(existing));

        SupportTicketResponse response = service.updateCurrentWorkspace(
                "ticket-123",
                new UpdateSupportTicketRequest("resolved", null, null, null)
        );

        assertThat(response.status()).isEqualTo("resolved");
        assertThat(response.category()).isEqualTo("billing");
        assertThat(response.severity()).isEqualTo("high");
    }

    @Test
    @DisplayName("shouldCapSafeLimitAt200")
    void shouldCapSafeLimitAt200() {
        List<SupportTicketJpaEntity> manyTickets = new ArrayList<>();
        for (int i = 0; i < 250; i++) {
            SupportTicketJpaEntity t = buildExistingTicket();
            t.setId("ticket-" + i);
            manyTickets.add(t);
        }
        when(repository.findByWorkspaceIdOrderByUpdatedAtDesc(any(Long.class), any(org.springframework.data.domain.Pageable.class)))
                .thenAnswer(invocation -> {
                    org.springframework.data.domain.Pageable pageable = invocation.getArgument(1);
                    return manyTickets.stream().limit(pageable.getPageSize()).toList();
                });

        List<SupportTicketResponse> resultCapped = service.listCurrentWorkspace(500);
        assertThat(resultCapped).hasSize(200);

        List<SupportTicketResponse> resultDefault = service.listCurrentWorkspace(0);
        assertThat(resultDefault).hasSize(20);
    }

    @Test
    @DisplayName("shouldThrowNotFoundForMissingTicket")
    void shouldThrowNotFoundForMissingTicket() {
        when(repository.findByIdAndWorkspaceId(any(), eq(1L))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCurrentWorkspaceTicket("ticket-nonexistent"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("shouldRecordUsageEventOnCreate")
    void shouldRecordUsageEventOnCreate() {
        service.createCurrentWorkspace(
                new CreateSupportTicketRequest("Title", "Desc", "billing", "low")
        );

        assertThat(ledgerService.recordedEventType).isEqualTo("support.ticket_created");
        assertThat(ledgerService.recordedResourceType).isEqualTo("support_ticket");
    }

    @Test
    @DisplayName("shouldRecordAuditLogOnCreateAndUpdate")
    void shouldRecordAuditLogOnCreateAndUpdate() {
        SupportTicketResponse created = service.createCurrentWorkspace(
                new CreateSupportTicketRequest("Title", "Desc", "billing", "low")
        );

        assertThat(auditLogService.recordedActions).contains("created");

        SupportTicketJpaEntity existing = buildExistingTicket();
        existing.setId(created.id());
        when(repository.findByIdAndWorkspaceId(created.id(), 1L)).thenReturn(Optional.of(existing));

        service.updateCurrentWorkspace(
                created.id(),
                new UpdateSupportTicketRequest("resolved", null, null, null)
        );

        assertThat(auditLogService.recordedActions).contains("created", "updated");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private SupportTicketJpaEntity buildExistingTicket() {
        SupportTicketJpaEntity ticket = new SupportTicketJpaEntity();
        ticket.setId("ticket-123");
        ticket.setWorkspaceId(1L);
        ticket.setCreatedByUserId(1L);
        ticket.setTitle("Existing ticket");
        ticket.setDescription("Some description");
        ticket.setCategory("operational");
        ticket.setSeverity("medium");
        ticket.setStatus("open");
        ticket.setResolutionNote(null);
        return ticket;
    }

    // ── stubs / spies ───────────────────────────────────────────────────────

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
            return 1L;
        }

        @Override
        public Long getActorUserIdOrNull() {
            return 1L;
        }

        @Override
        public String getActorName() {
            return "Lume Operator";
        }
    }

    private static final class SpyAuditLogService extends AuditLogService {
        final List<String> recordedActions = new ArrayList<>();

        private SpyAuditLogService() {
            super(null, null, new ObjectMapper());
        }

        @Override
        public void record(String entityType, String entityId, String action, Object payload) {
            recordedActions.add(action);
        }
    }

    private static final class NoOpSupportTicketSlaService extends SupportTicketSlaService {
        private NoOpSupportTicketSlaService() {
            super(new com.lume.infrastructure.config.SlaProperties(), java.time.Clock.systemUTC());
        }

        @Override
        public java.time.LocalDateTime resolveTargetAt(SupportTicketJpaEntity ticket) {
            return null;
        }

        @Override
        public boolean isBreached(SupportTicketJpaEntity ticket) {
            return false;
        }
    }

    private static final class SpyWorkspaceLedgerService extends NoOpWorkspaceLedgerService {
        String recordedEventType;
        String recordedResourceType;

        @Override
        public void recordUsageEvent(String eventType, String resourceType, String resourceId, String details) {
            this.recordedEventType = eventType;
            this.recordedResourceType = resourceType;
        }
    }
}
