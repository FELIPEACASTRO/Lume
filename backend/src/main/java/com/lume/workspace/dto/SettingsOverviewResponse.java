package com.lume.workspace.dto;

import java.util.List;

public record SettingsOverviewResponse(
        String organizationName,
        String workspaceName,
        String roleLabel,
        int unreadNotifications,
        int knowledgeSources,
        UsageSummaryResponse usage,
        SettingsPreferencesResponse preferences,
        List<SettingsSectionResponse> sections
) {
}
