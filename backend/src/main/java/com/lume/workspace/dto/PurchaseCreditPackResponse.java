package com.lume.workspace.dto;

public record PurchaseCreditPackResponse(
        WorkspaceSubscriptionResponse subscription,
        InvoiceResponse invoice,
        PaymentEventResponse paymentEvent
) {
}
