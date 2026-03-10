package com.lume.workspace.dto;

public record CreditLedgerEntryResponse(
        Long id,
        String entryType,
        String sourceType,
        String sourceId,
        int creditsDelta,
        Integer balanceAfter,
        String note,
        String createdAt
) {
}
