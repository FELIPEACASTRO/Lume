package com.lume.workspace.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateWorkspaceBudgetRequest(
        @Size(max = 120, message = "costCenter must be at most 120 characters")
        String costCenter,
        @Pattern(regexp = "showback|chargeback", message = "chargebackMode must be showback or chargeback")
        String chargebackMode,
        @PositiveOrZero(message = "softLimitCredits must be positive or zero")
        Integer softLimitCredits,
        @PositiveOrZero(message = "hardLimitCredits must be positive or zero")
        Integer hardLimitCredits
) {
}
