package com.lume.workspace.dto;

public record SettingsUiOptionItemResponse(
        String code,
        String label,
        String description,
        boolean defaultOption
) {
}
