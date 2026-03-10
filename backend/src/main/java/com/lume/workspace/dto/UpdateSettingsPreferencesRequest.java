package com.lume.workspace.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateSettingsPreferencesRequest(
        @Pattern(regexp = "light|dark", message = "appearance must be light or dark")
        String appearance,
        @Size(max = 20, message = "languageCode must be at most 20 characters")
        String languageCode,
        Boolean emailUpdates,
        Boolean productUpdates
) {
}
