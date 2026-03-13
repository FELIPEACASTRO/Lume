package com.lume.workspace.dto;

public record SettingsComplianceSummaryResponse(
        boolean billingWebhookSecretConfigured,
        boolean auditTrailEnabled,
        boolean darkWebMonitoringEnabled,
        boolean threatIntelRestrictedToAdmins,
        boolean byokValidationEnabled,
        String retentionPolicyStatus,
        Integer retentionDays,
        String accessReviewStatus,
        Integer accessReviewFrequencyDays,
        boolean consentTrackingEnabled,
        String termsVersion,
        String updatedAt,
        String note
) {
}
