package com.lume.workspace.service;

import com.lume.workspace.dto.CostLedgerEntryResponse;
import com.lume.workspace.dto.CreditLedgerEntryResponse;
import com.lume.workspace.dto.FinopsScorecardResponse;
import com.lume.workspace.dto.UsageEventResponse;

import java.util.List;

/**
 * Test double to avoid wiring persistence for ledger side-effects in unit tests.
 */
public class NoOpWorkspaceLedgerService extends WorkspaceLedgerService {

    public NoOpWorkspaceLedgerService() {
        super(null, null, null, null, null, null);
    }

    @Override
    public void recordUsageEvent(String eventType, String resourceType, String resourceId, String details) {
    }

    @Override
    public void recordCreditEntry(
            Long workspaceId,
            String entryType,
            String sourceType,
            String sourceId,
            int creditsDelta,
            String note
    ) {
    }

    @Override
    public void recordCostEntry(CostLedgerRecord record) {
    }

    @Override
    public List<UsageEventResponse> listUsageEvents(int limit) {
        return List.of();
    }

    @Override
    public List<CreditLedgerEntryResponse> listCreditEntries(int limit) {
        return List.of();
    }

    @Override
    public List<CostLedgerEntryResponse> listCostEntries(int limit) {
        return List.of();
    }

    @Override
    public FinopsScorecardResponse scorecard() {
        return new FinopsScorecardResponse(null, null, null, null, null, null, null, null, 0, null);
    }
}
