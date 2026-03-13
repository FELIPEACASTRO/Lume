package com.lume.workspace.dto;

public record SettingsGovernanceSummaryResponse(
        long openSupportTickets,
        long criticalOpenSupportTickets,
        long overdueSupportTickets,
        long byokConnections,
        long healthyByokConnections,
        int coreLiveProviders,
        int supportedRestrictedProviders,
        int blockedProviders,
        String lastReconciliationStatus,
        String lastReconciliationExecutedAt,
        String note
) {
}
