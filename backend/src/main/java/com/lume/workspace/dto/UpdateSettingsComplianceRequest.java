package com.lume.workspace.dto;

import jakarta.validation.constraints.Size;

public record UpdateSettingsComplianceRequest(
        String retentionPolicyStatus,
        Integer retentionDays,
        String accessReviewStatus,
        Integer accessReviewFrequencyDays,
        Boolean consentTrackingEnabled,
        @Size(max = 64, message = "termsVersion aceita ate 64 caracteres")
        String termsVersion
) {
}
