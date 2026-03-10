package com.lume.workspace.controller;

import com.lume.workspace.dto.HomeOverviewResponse;
import com.lume.workspace.service.HomeOverviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/v1/home", "/api/v1/home"})
public class VersionedHomeController {

    private final HomeOverviewService homeOverviewService;

    public VersionedHomeController(HomeOverviewService homeOverviewService) {
        this.homeOverviewService = homeOverviewService;
    }

    @GetMapping("/overview")
    public ResponseEntity<HomeOverviewResponse> overview() {
        return ResponseEntity.ok(homeOverviewService.getOverview());
    }
}
