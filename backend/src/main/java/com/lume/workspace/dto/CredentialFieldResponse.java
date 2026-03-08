package com.lume.workspace.dto;

public record CredentialFieldResponse(
        String key,
        String label,
        String envVar,
        boolean required,
        boolean secret,
        boolean configured,
        String description
) {
}
