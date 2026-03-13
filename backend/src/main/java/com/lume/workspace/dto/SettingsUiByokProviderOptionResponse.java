package com.lume.workspace.dto;

public record SettingsUiByokProviderOptionResponse(
        String providerCode,
        String providerName,
        String secretRefSuggestion
) {
}
