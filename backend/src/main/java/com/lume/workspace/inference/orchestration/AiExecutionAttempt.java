package com.lume.workspace.inference.orchestration;

public record AiExecutionAttempt(
        String providerCode,
        String status,
        String error,
        long latencyMs
) {
}
