package com.lume.workspace.controller;

import com.lume.workspace.dto.BudgetSummaryResponse;
import com.lume.workspace.dto.UpdateWorkspaceBudgetRequest;
import com.lume.workspace.service.WorkspaceBudgetService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/v1/budgets", "/api/v1/budgets"})
public class VersionedBudgetController {

    private final WorkspaceBudgetService workspaceBudgetService;

    public VersionedBudgetController(WorkspaceBudgetService workspaceBudgetService) {
        this.workspaceBudgetService = workspaceBudgetService;
    }

    @GetMapping("/current")
    public ResponseEntity<BudgetSummaryResponse> current() {
        return ResponseEntity.ok(workspaceBudgetService.getCurrentBudget());
    }

    @PatchMapping("/current")
    public ResponseEntity<BudgetSummaryResponse> updateCurrent(
            @Valid @RequestBody UpdateWorkspaceBudgetRequest request
    ) {
        return ResponseEntity.ok(workspaceBudgetService.updateCurrentBudget(request));
    }
}
