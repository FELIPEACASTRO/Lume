package com.lume.workspace.dto;

public record BillingWebhookProcessResponse(
        String gatewayEventId,
        String processingStatus,
        boolean duplicate,
        String eventType,
        String status,
        String invoiceNumber,
        String subscriptionStatus
) {
}
