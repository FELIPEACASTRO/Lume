package com.lume.workspace.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lume.workspace.dto.BootstrapSetupRequest;
import com.lume.workspace.entity.WorkspaceOnboardingProfileJpaEntity;
import com.lume.workspace.repository.WorkspaceOnboardingProfileJpaRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("WorkspaceOnboardingService - Unit Tests")
class WorkspaceOnboardingServiceTest {

    private WorkspaceOnboardingProfileJpaRepository onboardingRepository;
    private SpyAuditLogService auditLogService;
    private SpyLedgerService ledgerService;
    private WorkspaceOnboardingService service;

    @BeforeEach
    void setUp() {
        onboardingRepository = mock(WorkspaceOnboardingProfileJpaRepository.class);
        auditLogService = new SpyAuditLogService();
        ledgerService = new SpyLedgerService();

        when(onboardingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(onboardingRepository.findByWorkspaceId(1L)).thenReturn(Optional.of(defaultOnboarding()));

        service = new WorkspaceOnboardingService(
                onboardingRepository,
                new StubWorkspaceContextService(),
                auditLogService,
                ledgerService,
                new StubWorkspaceSubscriptionService(ledgerService)
        );
    }

    @Test
    @DisplayName("initializeWorkspace sets onboarding started and delegates subscription init")
    void shouldInitializeWorkspaceCorrectly() {
        when(onboardingRepository.findByWorkspaceId(1L)).thenReturn(Optional.empty());

        service.initializeWorkspace(1L, new BootstrapSetupRequest(
                "Org", "Workspace", "Admin", "admin@test.com", "admin123",
                "operations", "solo_operator", "starter"
        ));

        assertThat(ledgerService.creditRecorded).isTrue();
        assertThat(ledgerService.creditSourceType).isEqualTo("subscription_onboarding");
    }

    @Test
    @DisplayName("advanceCurrentOnboarding advances when target rank is higher")
    void shouldAdvanceOnboardingWhenRankIsHigher() {
        WorkspaceOnboardingProfileJpaEntity onboarding = defaultOnboarding();
        onboarding.setActivationStatus("started");
        when(onboardingRepository.findByWorkspaceId(1L)).thenReturn(Optional.of(onboarding));

        var response = service.advanceCurrentOnboarding(
                "profile_selected",
                "Profile selecionado",
                "test.event"
        );

        assertThat(response.activationStatus()).isEqualTo("profile_selected");
        assertThat(auditLogService.recordedActions).contains("step_transition");
    }

    @Test
    @DisplayName("advanceCurrentOnboarding does NOT regress when target rank is lower")
    void shouldNotRegressOnboarding() {
        WorkspaceOnboardingProfileJpaEntity onboarding = defaultOnboarding();
        onboarding.setActivationStatus("first_project_created");
        when(onboardingRepository.findByWorkspaceId(1L)).thenReturn(Optional.of(onboarding));

        var response = service.advanceCurrentOnboarding(
                "started",
                "Tentativa de regressao",
                "test.event"
        );

        assertThat(response.activationStatus()).isEqualTo("first_project_created");
        assertThat(auditLogService.recordedActions).isEmpty();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private WorkspaceOnboardingProfileJpaEntity defaultOnboarding() {
        WorkspaceOnboardingProfileJpaEntity entity = new WorkspaceOnboardingProfileJpaEntity();
        entity.setWorkspaceId(1L);
        entity.setPrimaryUseCase("operations");
        entity.setWorkStyle("team");
        entity.setActivationStatus("started");
        entity.setActivationNote("Onboarding iniciado.");
        return entity;
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

    private static final class SpyAuditLogService extends AuditLogService {
        final List<String> recordedActions = new ArrayList<>();

        private SpyAuditLogService() {
            super(null, null, new ObjectMapper());
        }

        @Override
        public void record(String entityType, String entityId, String action, Object payload) {
            recordedActions.add(action);
        }

        @Override
        public void recordExplicit(Long orgId, Long wsId, Long userId,
                                   String entityType, String entityId, String action, Object payload) {
            recordedActions.add(action);
        }
    }

    private static final class SpyLedgerService extends NoOpWorkspaceLedgerService {
        boolean creditRecorded = false;
        String creditSourceType;

        @Override
        public void recordCreditEntry(Long workspaceId, String entryType, String sourceType,
                                      String sourceId, int creditsDelta, String note) {
            creditRecorded = true;
            creditSourceType = sourceType;
        }
    }

    private static final class StubWorkspaceSubscriptionService extends WorkspaceSubscriptionService {
        private final SpyLedgerService ledgerService;

        private StubWorkspaceSubscriptionService(SpyLedgerService ledgerService) {
            super(null, null, null, null);
            this.ledgerService = ledgerService;
        }

        @Override
        public void initializeSubscription(Long workspaceId, String selectedPlan) {
            ledgerService.recordCreditEntry(workspaceId, "grant", "subscription_onboarding",
                    "workspace:" + workspaceId, 500, "Creditos iniciais.");
        }
    }
}
