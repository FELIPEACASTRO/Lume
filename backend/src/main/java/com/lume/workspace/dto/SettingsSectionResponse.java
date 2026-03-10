package com.lume.workspace.dto;

public record SettingsSectionResponse(
        String key,
        String title,
        String description,
        String availability
) {
}
