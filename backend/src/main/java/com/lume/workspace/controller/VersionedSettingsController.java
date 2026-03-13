package com.lume.workspace.controller;

import com.lume.workspace.dto.SettingsOverviewResponse;
import com.lume.workspace.dto.SettingsPreferencesResponse;
import com.lume.workspace.dto.SettingsComplianceSummaryResponse;
import com.lume.workspace.dto.SettingsUiOptionsResponse;
import com.lume.workspace.dto.AuditFeedEntryResponse;
import com.lume.workspace.dto.UpdateSettingsPreferencesRequest;
import com.lume.workspace.dto.UpdateSettingsComplianceRequest;
import com.lume.workspace.service.SettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Settings", description = "Workspace settings, preferences, compliance, and audit feed")
@RestController
@RequestMapping("/v1/settings")
public class VersionedSettingsController {

    private final SettingsService settingsService;

    public VersionedSettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @Operation(summary = "Get settings overview", description = "Returns the consolidated settings overview for the current workspace.")
    @ApiResponse(responseCode = "200", description = "Settings overview returned successfully")
    @GetMapping("/overview")
    public ResponseEntity<SettingsOverviewResponse> overview() {
        return ResponseEntity.ok(settingsService.getOverview());
    }

    @Operation(summary = "Get UI options", description = "Returns available UI configuration options for settings forms.")
    @ApiResponse(responseCode = "200", description = "UI options returned successfully")
    @GetMapping("/ui-options")
    public ResponseEntity<SettingsUiOptionsResponse> uiOptions() {
        return ResponseEntity.ok(settingsService.getUiOptions());
    }

    @Operation(summary = "Get preferences", description = "Returns the current workspace preferences.")
    @ApiResponse(responseCode = "200", description = "Preferences returned successfully")
    @GetMapping("/preferences")
    public ResponseEntity<SettingsPreferencesResponse> preferences() {
        return ResponseEntity.ok(settingsService.getPreferences());
    }

    @Operation(summary = "Update preferences", description = "Patches the current workspace preferences with the provided fields.")
    @ApiResponse(responseCode = "200", description = "Preferences updated successfully")
    @PatchMapping("/preferences")
    public ResponseEntity<SettingsPreferencesResponse> updatePreferences(
            @Valid @RequestBody UpdateSettingsPreferencesRequest request
    ) {
        return ResponseEntity.ok(settingsService.updatePreferences(request));
    }

    @Operation(summary = "Get compliance summary", description = "Returns the compliance settings and status for the current workspace.")
    @ApiResponse(responseCode = "200", description = "Compliance summary returned successfully")
    @GetMapping("/compliance")
    public ResponseEntity<SettingsComplianceSummaryResponse> compliance() {
        return ResponseEntity.ok(settingsService.getCompliance());
    }

    @Operation(summary = "Update compliance settings", description = "Patches the compliance configuration for the current workspace.")
    @ApiResponse(responseCode = "200", description = "Compliance settings updated successfully")
    @PatchMapping("/compliance")
    public ResponseEntity<SettingsComplianceSummaryResponse> updateCompliance(
            @Valid @RequestBody UpdateSettingsComplianceRequest request
    ) {
        return ResponseEntity.ok(settingsService.updateCompliance(request));
    }

    @Operation(summary = "Get audit feed", description = "Returns recent audit log entries for the current workspace.")
    @ApiResponse(responseCode = "200", description = "Audit feed returned successfully")
    @GetMapping("/audit-feed")
    public ResponseEntity<List<AuditFeedEntryResponse>> auditFeed(
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(settingsService.getAuditFeed(limit));
    }
}
