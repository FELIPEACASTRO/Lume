package com.lume.workspace.dto;

import java.math.BigDecimal;

public record SettingsUiCreditPackOptionResponse(
        String packCode,
        int credits,
        BigDecimal amountBrl,
        String description,
        boolean defaultOption
) {
}
