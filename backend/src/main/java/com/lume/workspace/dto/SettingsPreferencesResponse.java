package com.lume.workspace.dto;

public record SettingsPreferencesResponse(
        String appearance,
        String languageCode,
        boolean emailUpdates,
        boolean productUpdates
) {
}
