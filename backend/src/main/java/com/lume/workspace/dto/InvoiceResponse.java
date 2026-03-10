package com.lume.workspace.dto;

import java.math.BigDecimal;

public record InvoiceResponse(
        Long id,
        String invoiceNumber,
        String status,
        BigDecimal amountBrl,
        String currency,
        String dueAt,
        String paidAt,
        String description,
        String createdAt
) {
}
