package com.lume.workspace.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lume.domain.exception.BusinessRuleException;
import com.lume.infrastructure.config.SecurityComplianceProperties;
import com.lume.workspace.dto.ProviderStatusResponse;
import com.lume.workspace.dto.SettingsComplianceSummaryResponse;
import com.lume.workspace.dto.UpdateSettingsComplianceRequest;
import com.lume.workspace.entity.WorkspaceComplianceSettingJpaEntity;
import com.lume.workspace.inference.security.EnvironmentSecretResolver;
import com.lume.workspace.inference.security.SecretResolver;
import com.lume.workspace.repository.WorkspaceComplianceSettingJpaRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ComplianceSettingsService - Unit Tests")
class ComplianceSettingsServiceTest {

    private WorkspaceComplianceSettingJpaRepository complianceSettingRepository;
    private SecretResolver secretResolver;
    private SecurityComplianceProperties securityComplianceProperties;
    private SpyAuditLogService auditLogService;
    private StubProviderCatalogService stubProviderCatalogService;
    private ComplianceSettingsService service;

    @BeforeEach
    void setUp() {
        complianceSettingRepository = mock(WorkspaceComplianceSettingJpaRepository.class);
        secretResolver = mock(SecretResolver.class);
        securityComplianceProperties = new SecurityComplianceProperties();
        auditLogService = new SpyAuditLogService();
        stubProviderCatalogService = new StubProviderCatalogService();

        when(complianceSettingRepository.save(any(WorkspaceComplianceSettingJpaEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service = new ComplianceSettingsService(
                new StubWorkspaceContextService(),
                auditLogService,
                complianceSettingRepository,
                stubProviderCatalogService,
                secretResolver,
                securityComplianceProperties
        );
    }

    @Test
    @DisplayName("should return defaults for new workspace")
    void shouldReturnDefaultsForNewWorkspace() {
        when(complianceSettingRepository.findById(1L)).thenReturn(Optional.empty());

        SettingsComplianceSummaryResponse response = service.getCompliance();

        assertThat(response.retentionPolicyStatus()).isEqualTo("not_configured");
        assertThat(response.accessReviewStatus()).isEqualTo("not_configured");
        assertThat(response.consentTrackingEnabled()).isFalse();
    }

    @Test
    @DisplayName("should update retention to configured with 30 days")
    void shouldUpdateRetentionToConfigured() {
        stubExistingEntity(defaultEntity());

        SettingsComplianceSummaryResponse response = service.updateCompliance(
                new UpdateSettingsComplianceRequest("configured", 30, null, null, null, null)
        );

        assertThat(response.retentionPolicyStatus()).isEqualTo("configured");
        assertThat(response.retentionDays()).isEqualTo(30);
        verify(complianceSettingRepository).save(any(WorkspaceComplianceSettingJpaEntity.class));
    }

    @Test
    @DisplayName("should reject configured retention with zero days")
    void shouldRejectConfiguredRetentionWithZeroDays() {
        stubExistingEntity(defaultEntity());

        assertThatThrownBy(() -> service.updateCompliance(
                new UpdateSettingsComplianceRequest("configured", 0, null, null, null, null)
        )).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("should reject configured retention with null days")
    void shouldRejectConfiguredRetentionWithNullDays() {
        stubExistingEntity(defaultEntity());

        assertThatThrownBy(() -> service.updateCompliance(
                new UpdateSettingsComplianceRequest("configured", null, null, null, null, null)
        )).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("should clear retention days when not configured")
    void shouldClearRetentionDaysWhenNotConfigured() {
        WorkspaceComplianceSettingJpaEntity entity = defaultEntity();
        entity.setRetentionPolicyStatus("configured");
        entity.setRetentionDays(90);
        stubExistingEntity(entity);

        service.updateCompliance(
                new UpdateSettingsComplianceRequest("not_configured", null, null, null, null, null)
        );

        ArgumentCaptor<WorkspaceComplianceSettingJpaEntity> captor =
                ArgumentCaptor.forClass(WorkspaceComplianceSettingJpaEntity.class);
        verify(complianceSettingRepository).save(captor.capture());
        assertThat(captor.getValue().getRetentionDays()).isNull();
    }

    @Test
    @DisplayName("should reject configured access review with zero frequency")
    void shouldRejectConfiguredAccessReviewWithZeroFreq() {
        stubExistingEntity(defaultEntity());

        assertThatThrownBy(() -> service.updateCompliance(
                new UpdateSettingsComplianceRequest(null, null, "configured", 0, null, null)
        )).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("should normalize policy status from dash to underscore")
    void shouldNormalizePolicyStatusFromDash() {
        stubExistingEntity(defaultEntity());

        service.updateCompliance(
                new UpdateSettingsComplianceRequest("not-configured", null, null, null, null, null)
        );

        ArgumentCaptor<WorkspaceComplianceSettingJpaEntity> captor =
                ArgumentCaptor.forClass(WorkspaceComplianceSettingJpaEntity.class);
        verify(complianceSettingRepository).save(captor.capture());
        assertThat(captor.getValue().getRetentionPolicyStatus()).isEqualTo("not_configured");
    }

    @Test
    @DisplayName("should reject invalid policy status")
    void shouldRejectInvalidPolicyStatus() {
        stubExistingEntity(defaultEntity());

        assertThatThrownBy(() -> service.updateCompliance(
                new UpdateSettingsComplianceRequest("active", null, null, null, null, null)
        )).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("should update consent and terms version")
    void shouldUpdateConsentAndTerms() {
        stubExistingEntity(defaultEntity());

        service.updateCompliance(
                new UpdateSettingsComplianceRequest(null, null, null, null, true, "v2026.03")
        );

        ArgumentCaptor<WorkspaceComplianceSettingJpaEntity> captor =
                ArgumentCaptor.forClass(WorkspaceComplianceSettingJpaEntity.class);
        verify(complianceSettingRepository).save(captor.capture());
        assertThat(captor.getValue().isConsentTrackingEnabled()).isTrue();
        assertThat(captor.getValue().getTermsVersion()).isEqualTo("v2026.03");
    }

    @Test
    @DisplayName("should clear blank terms version to null")
    void shouldClearBlankTermsVersion() {
        stubExistingEntity(defaultEntity());

        service.updateCompliance(
                new UpdateSettingsComplianceRequest(null, null, null, null, null, " ")
        );

        ArgumentCaptor<WorkspaceComplianceSettingJpaEntity> captor =
                ArgumentCaptor.forClass(WorkspaceComplianceSettingJpaEntity.class);
        verify(complianceSettingRepository).save(captor.capture());
        assertThat(captor.getValue().getTermsVersion()).isNull();
    }

    @Test
    @DisplayName("should record audit on update with entity_type=workspace_compliance")
    void shouldRecordAuditOnUpdate() {
        stubExistingEntity(defaultEntity());

        service.updateCompliance(
                new UpdateSettingsComplianceRequest(null, null, null, null, true, null)
        );

        assertThat(auditLogService.lastEntityType).isEqualTo("workspace_compliance");
        assertThat(auditLogService.lastEntityId).isEqualTo("1");
        assertThat(auditLogService.lastAction).isEqualTo("updated");
        assertThat(auditLogService.lastPayload).isNotNull();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private WorkspaceComplianceSettingJpaEntity defaultEntity() {
        WorkspaceComplianceSettingJpaEntity entity = new WorkspaceComplianceSettingJpaEntity();
        entity.setWorkspaceId(1L);
        entity.setRetentionPolicyStatus("not_configured");
        entity.setAccessReviewStatus("not_configured");
        entity.setConsentTrackingEnabled(false);
        return entity;
    }

    private void stubExistingEntity(WorkspaceComplianceSettingJpaEntity entity) {
        when(complianceSettingRepository.findById(1L)).thenReturn(Optional.of(entity));
    }

    // ── test doubles ─────────────────────────────────────────────────────────

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
        public Long getOrganizationId() {
            return 1L;
        }

        @Override
        public String getActorName() {
            return "Lume Operator";
        }
    }

    private static final class SpyAuditLogService extends AuditLogService {

        String lastEntityType;
        String lastEntityId;
        String lastAction;
        Object lastPayload;

        private SpyAuditLogService() {
            super(null, null, new ObjectMapper());
        }

        @Override
        public void record(String entityType, String entityId, String action, Object payload) {
            this.lastEntityType = entityType;
            this.lastEntityId = entityId;
            this.lastAction = action;
            this.lastPayload = payload;
        }
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
}
