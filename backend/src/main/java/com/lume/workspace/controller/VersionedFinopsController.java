package com.lume.workspace.controller;

import com.lume.workspace.dto.CostLedgerEntryResponse;
import com.lume.workspace.dto.CreditLedgerEntryResponse;
import com.lume.workspace.dto.FinopsAnomalyResponse;
import com.lume.workspace.dto.FinopsReconciliationRunEntryResponse;
import com.lume.workspace.dto.FinopsReconciliationResponse;
import com.lume.workspace.dto.FinopsReconciliationRunRequest;
import com.lume.workspace.dto.FinopsScorecardResponse;
import com.lume.workspace.dto.UsageEventResponse;
import com.lume.workspace.service.WorkspaceContextService;
import com.lume.workspace.service.WorkspaceFinopsAnomalyService;
import com.lume.workspace.service.WorkspaceFinopsReconciliationService;
import com.lume.workspace.service.WorkspaceLedgerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/finops")
public class VersionedFinopsController {

    private final WorkspaceLedgerService workspaceLedgerService;
    private final WorkspaceFinopsAnomalyService workspaceFinopsAnomalyService;
    private final WorkspaceFinopsReconciliationService workspaceFinopsReconciliationService;
    private final WorkspaceContextService workspaceContextService;

    public VersionedFinopsController(
            WorkspaceLedgerService workspaceLedgerService,
            WorkspaceFinopsAnomalyService workspaceFinopsAnomalyService,
            WorkspaceFinopsReconciliationService workspaceFinopsReconciliationService,
            WorkspaceContextService workspaceContextService
    ) {
        this.workspaceLedgerService = workspaceLedgerService;
        this.workspaceFinopsAnomalyService = workspaceFinopsAnomalyService;
        this.workspaceFinopsReconciliationService = workspaceFinopsReconciliationService;
        this.workspaceContextService = workspaceContextService;
    }

    @GetMapping("/scorecard")
    public ResponseEntity<FinopsScorecardResponse> scorecard() {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_BUDGETS_READ);
        return ResponseEntity.ok(workspaceLedgerService.scorecard());
    }

    @GetMapping("/anomalies")
    public ResponseEntity<List<FinopsAnomalyResponse>> anomalies() {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_BUDGETS_READ);
        return ResponseEntity.ok(workspaceFinopsAnomalyService.listCurrentWorkspace());
    }

    @GetMapping("/ledgers/credits")
    public ResponseEntity<List<CreditLedgerEntryResponse>> creditLedger(
            @RequestParam(defaultValue = "20") int limit
    ) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_BUDGETS_READ);
        return ResponseEntity.ok(workspaceLedgerService.listCreditEntries(limit));
    }

    @GetMapping("/ledgers/costs")
    public ResponseEntity<List<CostLedgerEntryResponse>> costLedger(
            @RequestParam(defaultValue = "20") int limit
    ) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_BUDGETS_READ);
        return ResponseEntity.ok(workspaceLedgerService.listCostEntries(limit));
    }

    @GetMapping("/events/usage")
    public ResponseEntity<List<UsageEventResponse>> usageEvents(
            @RequestParam(defaultValue = "20") int limit
    ) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_WORKSPACE_READ);
        return ResponseEntity.ok(workspaceLedgerService.listUsageEvents(limit));
    }

    @GetMapping("/reconciliation")
    public ResponseEntity<FinopsReconciliationResponse> reconciliation() {
        return ResponseEntity.ok(workspaceFinopsReconciliationService.previewCurrentWorkspace());
    }

    @PostMapping("/reconciliation/run")
    public ResponseEntity<FinopsReconciliationResponse> runReconciliation(
            @RequestBody(required = false) FinopsReconciliationRunRequest request
    ) {
        return ResponseEntity.ok(workspaceFinopsReconciliationService.runCurrentWorkspace(request));
    }

    @GetMapping("/reconciliation/history")
    public ResponseEntity<List<FinopsReconciliationRunEntryResponse>> reconciliationHistory(
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(workspaceFinopsReconciliationService.listCurrentWorkspaceHistory(limit));
    }
}
