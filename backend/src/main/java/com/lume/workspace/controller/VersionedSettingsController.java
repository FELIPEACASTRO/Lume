package com.lume.workspace.controller;

import com.lume.workspace.dto.SettingsPreferencesResponse;
import com.lume.workspace.dto.UpdateSettingsPreferencesRequest;
import com.lume.workspace.service.SettingsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/v1/settings", "/api/v1/settings"})
public class VersionedSettingsController {

    private final SettingsService settingsService;

    public VersionedSettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping("/preferences")
    public ResponseEntity<SettingsPreferencesResponse> preferences() {
        return ResponseEntity.ok(settingsService.getPreferences());
    }

    @PatchMapping("/preferences")
    public ResponseEntity<SettingsPreferencesResponse> updatePreferences(
            @Valid @RequestBody UpdateSettingsPreferencesRequest request
    ) {
        return ResponseEntity.ok(settingsService.updatePreferences(request));
    }
}
