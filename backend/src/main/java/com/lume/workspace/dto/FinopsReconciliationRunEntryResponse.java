package com.lume.workspace.dto;

import java.math.BigDecimal;

public record FinopsReconciliationRunEntryResponse(
        Long id,
        String runMode,
        String reconciliationStatus,
        int subscriptionCredits,
        int ledgerCreditBalance,
        int creditDrift,
        long invoicesTotal,
        long invoicesPaid,
        BigDecimal invoicesPaidAmountBrl,
        long paymentEventsTotal,
        long paymentEventsProcessed,
        BigDecimal paymentEventsProcessedAmountBrl,
        long orphanPaymentEvents,
        long pendingPaymentEvents,
        boolean creditFixApplied,
        Integer creditFixDelta,
        String recommendation,
        String executedAt
) {
}
