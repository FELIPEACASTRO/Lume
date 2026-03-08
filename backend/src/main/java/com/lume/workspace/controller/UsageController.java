package com.lume.workspace.controller;

import com.lume.workspace.dto.UsageSummaryResponse;
import com.lume.workspace.service.UsageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/usage")
public class UsageController {

    private final UsageService usageService;

    public UsageController(UsageService usageService) {
        this.usageService = usageService;
    }

    @GetMapping("/summary")
    public ResponseEntity<UsageSummaryResponse> summary() {
        return ResponseEntity.ok(usageService.getSummary());
    }
}
