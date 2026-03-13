package com.lume.workspace.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lume.infrastructure.config.SlaProperties;
import com.lume.workspace.dto.ProviderResponse;
import com.lume.workspace.dto.ProviderStatusResponse;
import com.lume.workspace.dto.SessionContextResponse;
import com.lume.workspace.dto.SessionRoleResponse;
import com.lume.workspace.dto.SessionUserResponse;
import com.lume.workspace.dto.OrganizationResponse;
import com.lume.workspace.dto.SettingsComplianceSummaryResponse;
import com.lume.workspace.dto.UpdateSettingsComplianceRequest;
import com.lume.workspace.dto.UpdateSettingsPreferencesRequest;
import com.lume.workspace.dto.WorkspaceResponse;
import com.lume.workspace.entity.UserPreferenceJpaEntity;
import com.lume.workspace.entity.SupportTicketJpaEntity;
import com.lume.workspace.inference.security.EnvironmentSecretResolver;
import com.lume.workspace.repository.RoleJpaRepository;
import com.lume.workspace.repository.UserPreferenceJpaRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.env.MockEnvironment;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("SettingsService - Unit Tests")
class SettingsServiceTest {

    private UserPreferenceJpaRepository userPreferenceRepository;
    private RoleJpaRepository roleRepository;
    private SpyAuditLogService auditLogService;
    private StubComplianceSettingsService complianceSettingsService;
    private SettingsService service;

    @BeforeEach
    void setUp() {
        userPreferenceRepository = mock(UserPreferenceJpaRepository.class);
        roleRepository = mock(RoleJpaRepository.class);
        auditLogService = new SpyAuditLogService();
        complianceSettingsService = new StubComplianceSettingsService();

        when(userPreferenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userPreferenceRepository.findByUserId(1L)).thenReturn(Optional.of(defaultPreference()));

        service = new SettingsService(
                new StubWorkspaceContextService(),
                new StubNotificationService(),
                new StubUsageService(),
                new StubKnowledgeSourceService(),
                userPreferenceRepository,
                roleRepository,
                auditLogService,
                new StubWorkspaceCommercialService(),
                complianceSettingsService,
                new StubGovernanceSummaryService(),
                new StubProviderCatalogService()
        );
    }

    @Test
    @DisplayName("getOverview returns sections based on permissions")
    void shouldReturnOverviewWithBaseSections() {
        var overview = service.getOverview();

        assertThat(overview.organizationName()).isEqualTo("Test Org");
        assertThat(overview.workspaceName()).isEqualTo("Test Workspace");
        assertThat(overview.unreadNotifications()).isEqualTo(3);
        assertThat(overview.knowledgeSources()).isEqualTo(5);
        assertThat(overview.sections()).hasSizeGreaterThanOrEqualTo(3);
        assertThat(overview.sections().stream().map(s -> s.key()))
                .contains("conta", "configuracoes", "uso");
    }

    @Test
    @DisplayName("getPreferences returns default preferences when no saved preference")
    void shouldReturnDefaultPreferences() {
        when(userPreferenceRepository.findByUserId(1L)).thenReturn(Optional.empty());

        var prefs = service.getPreferences();

        assertThat(prefs.appearance()).isEqualTo("light");
        assertThat(prefs.languageCode()).isEqualTo("pt-BR");
    }

    @Test
    @DisplayName("updatePreferences saves appearance and records audit log")
    void shouldUpdatePreferencesAndAudit() {
        var request = new UpdateSettingsPreferencesRequest("dark", null, null, null);

        var result = service.updatePreferences(request);

        assertThat(result.appearance()).isEqualTo("dark");
        assertThat(auditLogService.recordedActions).contains("updated");
    }

    @Test
    @DisplayName("updatePreferences normalizes unknown appearance to light")
    void shouldNormalizeInvalidAppearanceToLight() {
        var request = new UpdateSettingsPreferencesRequest("auto", null, null, null);

        var result = service.updatePreferences(request);

        assertThat(result.appearance()).isEqualTo("light");
    }

    @Test
    @DisplayName("updateCompliance delegates to ComplianceSettingsService")
    void shouldDelegateComplianceUpdate() {
        var request = new UpdateSettingsComplianceRequest(
                "configured", 90, "configured", 30, true, "v1.0"
        );
        var expected = new SettingsComplianceSummaryResponse(
                true, true, false, false, false,
                "configured", 90, "configured", 30,
                true, "v1.0", "2026-03-12T12:00:00Z", "test"
        );
        complianceSettingsService.complianceUpdateResult = expected;

        var result = service.updateCompliance(request);

        assertThat(result).isSameAs(expected);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private UserPreferenceJpaEntity defaultPreference() {
        UserPreferenceJpaEntity entity = new UserPreferenceJpaEntity();
        entity.setUserId(1L);
        entity.setAppearance("light");
        entity.setLanguageCode("pt-BR");
        entity.setEmailUpdates(true);
        entity.setProductUpdates(true);
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
        @Override public Long getOrganizationId() { return 1L; }
        @Override public Long getActorUserIdOrNull() { return 1L; }
        @Override public String getActorName() { return "Lume Operator"; }
        @Override public String getOrganizationName() { return "Test Org"; }
        @Override public String getWorkspaceName() { return "Test Workspace"; }
        @Override public List<String> getCurrentPermissions() {
            return List.of(
                    WorkspaceContextService.PERMISSION_WORKSPACE_READ,
                    WorkspaceContextService.PERMISSION_SETTINGS_MANAGE
            );
        }
        @Override public SessionContextResponse getSession() {
            return new SessionContextResponse(
                    new SessionUserResponse(1L, "Lume Operator", "admin@test.com", "LO"),
                    new OrganizationResponse(1L, "Test Org", "test-org"),
                    new WorkspaceResponse(1L, "Test Workspace", "test-ws"),
                    new SessionRoleResponse("workspace_admin", "Admin", List.of())
            );
        }
    }

    private static final class SpyAuditLogService extends AuditLogService {
        final java.util.ArrayList<String> recordedActions = new java.util.ArrayList<>();

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

    private static final class StubNotificationService extends NotificationService {
        private StubNotificationService() {
            super(null, null);
        }

        @Override
        public int unreadCount() {
            return 3;
        }
    }

    private static final class StubUsageService extends UsageService {
        private StubUsageService() {
            super(null, null, null);
        }

        @Override
        public com.lume.workspace.dto.UsageSummaryResponse getSummary() {
            return null;
        }
    }

    private static final class StubKnowledgeSourceService extends KnowledgeSourceService {
        private StubKnowledgeSourceService() {
            super(null, null, null, null);
        }

        @Override
        public int countSources() {
            return 5;
        }
    }

    private static final class StubWorkspaceCommercialService extends WorkspaceCommercialService {
        private StubWorkspaceCommercialService() {
            super(null, null, null, null, null, null, null, null, null);
        }

        @Override
        public com.lume.workspace.dto.WorkspaceCommercialSummaryResponse getCurrentSummary() {
            return null;
        }
    }

    private static final class StubComplianceSettingsService extends ComplianceSettingsService {
        SettingsComplianceSummaryResponse complianceUpdateResult;

        private StubComplianceSettingsService() {
            super(null, null, null, null, null, null);
        }

        @Override
        public SettingsComplianceSummaryResponse complianceSummary() {
            return null;
        }

        @Override
        public SettingsComplianceSummaryResponse updateCompliance(UpdateSettingsComplianceRequest request) {
            return complianceUpdateResult;
        }
    }

    private static final class StubGovernanceSummaryService extends GovernanceSummaryService {
        private StubGovernanceSummaryService() {
            super(null, null, null, new StubProviderCatalogService(), new StubSupportTicketSlaService());
        }

        @Override
        public com.lume.workspace.dto.SettingsGovernanceSummaryResponse governanceSummary(Long workspaceId) {
            return null;
        }
    }

    private static final class StubProviderCatalogService extends ProviderCatalogService {
        private StubProviderCatalogService() {
            super(new EnvironmentSecretResolver(new MockEnvironment()));
        }

        @Override
        public List<ProviderResponse> listProviders() {
            return List.of();
        }

        @Override
        public List<ProviderStatusResponse> listProviderStatuses() {
            return List.of();
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
