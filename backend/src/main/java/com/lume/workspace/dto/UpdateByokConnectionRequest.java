package com.lume.workspace.dto;

public record UpdateByokConnectionRequest(
        String connectionName,
        String secretRef,
        String status,
        String scopeLabel
) {
}
