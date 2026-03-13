package com.lume.workspace.controller;

import com.lume.workspace.dto.HomeOverviewResponse;
import com.lume.workspace.service.HomeOverviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/home")
@Tag(name = "Home", description = "Workspace home overview and landing data")
public class VersionedHomeController {

    private final HomeOverviewService homeOverviewService;

    public VersionedHomeController(HomeOverviewService homeOverviewService) {
        this.homeOverviewService = homeOverviewService;
    }

    @GetMapping("/overview")
    @Operation(
            summary = "Get home overview",
            description = "Returns the consolidated home overview including workspace summary, recent activity, and quick stats."
    )
    @ApiResponse(responseCode = "200", description = "Home overview retrieved successfully")
    public ResponseEntity<HomeOverviewResponse> overview() {
        return ResponseEntity.ok(homeOverviewService.getOverview());
    }
}
