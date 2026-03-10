package com.lume.workspace.dto;

import java.math.BigDecimal;

public record PaymentEventResponse(
        Long id,
        Long invoiceId,
        String gatewayEventId,
        String eventType,
        String status,
        BigDecimal amountBrl,
        String occurredAt,
        String processedAt
) {
}
