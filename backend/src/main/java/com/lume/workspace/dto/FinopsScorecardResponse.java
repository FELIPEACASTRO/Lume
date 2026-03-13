package com.lume.workspace.dto;

public record FinopsScorecardResponse(
        Integer activationMinutesToFirstTaskMedian,
        Double d30RetentionRate,
        Double churnRate,
        Double marginContributionRate,
        Integer operationalNps,
        Long weeklyActiveUsers,
        Long monthlyActiveUsers,
        Double taskCompletionRate,
        Double inferenceSuccessRate,
        Double averageEstimatedCostUsdPerRun,
        long paidInvoicesCount,
        long paymentFailureCount,
        long orphanPaymentEvents,
        long pendingPaymentEvents,
        String reconciliationStatus,
        Integer creditDrift,
        String lastReconciledAt,
        long openSupportTickets,
        long criticalOpenSupportTickets,
        long overdueSupportTickets,
        long byokConnections,
        long healthyByokConnections,
        int coreLiveProviders,
        int supportedRestrictedProviders,
        int blockedProviders,
        int currentCreditBalance,
        String notes
) {
}
