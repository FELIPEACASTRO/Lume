package com.lume.workspace.dto;

public record ByokConnectionResponse(
        String id,
        String providerCode,
        String providerName,
        String connectionName,
        String secretRef,
        String scopeLabel,
        String status,
        String healthStatus,
        String lastValidatedAt,
        String lastError,
        String createdAt,
        String updatedAt
) {
}
