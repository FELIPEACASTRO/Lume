package com.lume.workspace.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PurchaseCreditPackRequest(
        @NotBlank(message = "packCode e obrigatorio")
        @Size(max = 64, message = "packCode nao pode ultrapassar 64 caracteres")
        String packCode,
        @Min(value = 1, message = "credits precisa ser maior que zero")
        int credits,
        @DecimalMin(value = "0.01", message = "amountBrl precisa ser maior que zero")
        BigDecimal amountBrl,
        @Size(max = 255, message = "description nao pode ultrapassar 255 caracteres")
        String description
) {
}
