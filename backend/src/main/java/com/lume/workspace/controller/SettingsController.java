package com.lume.workspace.controller;

import com.lume.workspace.dto.SettingsOverviewResponse;
import com.lume.workspace.service.SettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/settings")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping("/overview")
    public ResponseEntity<SettingsOverviewResponse> overview() {
        return ResponseEntity.ok(settingsService.getOverview());
    }
}
