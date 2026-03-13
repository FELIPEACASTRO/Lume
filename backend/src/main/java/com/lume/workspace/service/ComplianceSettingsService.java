package com.lume.workspace.service;

import com.lume.domain.exception.BusinessRuleException;
import com.lume.infrastructure.config.SecurityComplianceProperties;
import com.lume.workspace.dto.SettingsComplianceSummaryResponse;
import com.lume.workspace.dto.UpdateSettingsComplianceRequest;
import com.lume.workspace.entity.WorkspaceComplianceSettingJpaEntity;
import com.lume.workspace.repository.WorkspaceComplianceSettingJpaRepository;
import com.lume.workspace.inference.security.SecretResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Locale;

@Service
public class ComplianceSettingsService {

    private final WorkspaceContextService workspaceContextService;
    private final AuditLogService auditLogService;
    private final WorkspaceComplianceSettingJpaRepository complianceSettingRepository;
    private final ProviderCatalogService providerCatalogService;
    private final SecretResolver secretResolver;
    private final SecurityComplianceProperties securityComplianceProperties;

    public ComplianceSettingsService(
            WorkspaceContextService workspaceContextService,
            AuditLogService auditLogService,
            WorkspaceComplianceSettingJpaRepository complianceSettingRepository,
            ProviderCatalogService providerCatalogService,
            SecretResolver secretResolver,
            SecurityComplianceProperties securityComplianceProperties
    ) {
        this.workspaceContextService = workspaceContextService;
        this.auditLogService = auditLogService;
        this.complianceSettingRepository = complianceSettingRepository;
        this.providerCatalogService = providerCatalogService;
        this.secretResolver = secretResolver;
        this.securityComplianceProperties = securityComplianceProperties;
    }

    public SettingsComplianceSummaryResponse getCompliance() {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_SETTINGS_MANAGE);
        return complianceSummary();
    }

    @Transactional
    public SettingsComplianceSummaryResponse updateCompliance(UpdateSettingsComplianceRequest request) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_SETTINGS_MANAGE);
        WorkspaceComplianceSettingJpaEntity setting = getOrCreateComplianceSetting();

        if (request.retentionPolicyStatus() != null) {
            setting.setRetentionPolicyStatus(normalizePolicyStatus(request.retentionPolicyStatus()));
        }
        if (request.retentionDays() != null) {
            setting.setRetentionDays(request.retentionDays());
        }
        if (request.accessReviewStatus() != null) {
            setting.setAccessReviewStatus(normalizePolicyStatus(request.accessReviewStatus()));
        }
        if (request.accessReviewFrequencyDays() != null) {
            setting.setAccessReviewFrequencyDays(request.accessReviewFrequencyDays());
        }
        if (request.consentTrackingEnabled() != null) {
            setting.setConsentTrackingEnabled(request.consentTrackingEnabled());
        }
        if (request.termsVersion() != null) {
            setting.setTermsVersion(request.termsVersion().isBlank() ? null : request.termsVersion().trim());
        }

        validateComplianceSetting(setting);
        setting.setUpdatedByUserId(workspaceContextService.getActorUserIdOrNull());
        complianceSettingRepository.save(setting);

        LinkedHashMap<String, Object> complianceAuditPayload = new LinkedHashMap<>();
        complianceAuditPayload.put("retentionPolicyStatus", setting.getRetentionPolicyStatus());
        complianceAuditPayload.put("retentionDays", setting.getRetentionDays());
        complianceAuditPayload.put("accessReviewStatus", setting.getAccessReviewStatus());
        complianceAuditPayload.put("accessReviewFrequencyDays", setting.getAccessReviewFrequencyDays());
        complianceAuditPayload.put("consentTrackingEnabled", setting.isConsentTrackingEnabled());
        complianceAuditPayload.put("termsVersion", setting.getTermsVersion());

        auditLogService.record(
                "workspace_compliance",
                String.valueOf(setting.getWorkspaceId()),
                "updated",
                complianceAuditPayload
        );
        return complianceSummary();
    }

    public SettingsComplianceSummaryResponse complianceSummary() {
        WorkspaceComplianceSettingJpaEntity setting = getOrCreateComplianceSetting();
        boolean billingWebhookSecretConfigured = secretResolver.resolveOptional("BILLING_WEBHOOK_SECRET") != null;
        boolean threatIntelRestrictedToAdmins = providerCatalogService.listProviderStatuses().stream()
                .filter(provider -> "threat-intel".equalsIgnoreCase(provider.category()))
                .allMatch(provider -> provider.adminOnly());

        return new SettingsComplianceSummaryResponse(
                billingWebhookSecretConfigured,
                true,
                securityComplianceProperties.isDarkWebEnabled(),
                threatIntelRestrictedToAdmins,
                true,
                setting.getRetentionPolicyStatus(),
                setting.getRetentionDays(),
                setting.getAccessReviewStatus(),
                setting.getAccessReviewFrequencyDays(),
                setting.isConsentTrackingEnabled(),
                setting.getTermsVersion(),
                setting.getUpdatedAt() == null ? null : setting.getUpdatedAt().toString(),
                complianceNote(setting, billingWebhookSecretConfigured, threatIntelRestrictedToAdmins)
        );
    }

    private WorkspaceComplianceSettingJpaEntity getOrCreateComplianceSetting() {
        Long workspaceId = workspaceContextService.getWorkspaceId();
        return complianceSettingRepository.findById(workspaceId).orElseGet(() -> {
            WorkspaceComplianceSettingJpaEntity entity = new WorkspaceComplianceSettingJpaEntity();
            entity.setWorkspaceId(workspaceId);
            entity.setRetentionPolicyStatus("not_configured");
            entity.setAccessReviewStatus("not_configured");
            entity.setConsentTrackingEnabled(false);
            return complianceSettingRepository.save(entity);
        });
    }

    private void validateComplianceSetting(WorkspaceComplianceSettingJpaEntity setting) {
        if ("configured".equals(setting.getRetentionPolicyStatus())) {
            if (setting.getRetentionDays() == null || setting.getRetentionDays() < 1) {
                throw new BusinessRuleException("retentionDays deve ser maior que zero quando a politica de retencao estiver configurada.");
            }
        } else {
            setting.setRetentionDays(null);
        }
        if ("configured".equals(setting.getAccessReviewStatus())) {
            if (setting.getAccessReviewFrequencyDays() == null || setting.getAccessReviewFrequencyDays() < 1) {
                throw new BusinessRuleException("accessReviewFrequencyDays deve ser maior que zero quando access review estiver configurado.");
            }
        } else {
            setting.setAccessReviewFrequencyDays(null);
        }
    }

    private String normalizePolicyStatus(String value) {
        if (value == null || value.isBlank()) {
            return "not_configured";
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return switch (normalized) {
            case "not_configured", "configured" -> normalized;
            default -> throw new BusinessRuleException("status de politica deve ser not_configured ou configured.");
        };
    }

    private String complianceNote(
            WorkspaceComplianceSettingJpaEntity setting,
            boolean billingWebhookSecretConfigured,
            boolean threatIntelRestrictedToAdmins
    ) {
        String retentionNote = "configured".equals(setting.getRetentionPolicyStatus())
                ? "retencao em " + setting.getRetentionDays() + " dias"
                : "retencao nao configurada";
        String accessReviewNote = "configured".equals(setting.getAccessReviewStatus())
                ? "access review a cada " + setting.getAccessReviewFrequencyDays() + " dias"
                : "access review nao configurado";
        String consentNote = setting.isConsentTrackingEnabled() ? "consentimento rastreado" : "consentimento sem rastreio";
        String termsNote = setting.getTermsVersion() == null ? "termos sem versao registrada" : "termos " + setting.getTermsVersion();
        String webhookNote = billingWebhookSecretConfigured ? "webhook assinado ativo" : "webhook sem segredo";
        String threatIntelNote = threatIntelRestrictedToAdmins ? "threat-intel admin-only" : "threat-intel exposto";
        return String.join(" · ", retentionNote, accessReviewNote, consentNote, termsNote, webhookNote, threatIntelNote);
    }
}
