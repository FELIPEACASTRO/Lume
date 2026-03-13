package com.lume.workspace.dto;

import java.util.List;

public record SettingsUiOptionsResponse(
        List<SettingsUiOptionItemResponse> onboardingPrimaryUseCases,
        List<SettingsUiOptionItemResponse> onboardingWorkStyles,
        List<SettingsUiOptionItemResponse> memberRoles,
        List<SettingsUiOptionItemResponse> supportCategories,
        List<SettingsUiOptionItemResponse> supportSeverities,
        List<SettingsUiOptionItemResponse> supportStatuses,
        List<SettingsUiOptionItemResponse> knowledgeSourceTypes,
        List<SettingsUiCreditPackOptionResponse> billingCreditPacks,
        List<SettingsUiByokProviderOptionResponse> byokProviders,
        List<SettingsUiOptionItemResponse> byokScopeOptions,
        List<SettingsUiOptionItemResponse> complianceRetentionPolicyStatuses,
        List<SettingsUiOptionItemResponse> complianceAccessReviewStatuses
) {
}
