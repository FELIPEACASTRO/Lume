package com.lume.workspace.inference;

public record CredentialFieldDefinition(
        String key,
        String label,
        String envVar,
        boolean required,
        boolean secret,
        String description
) {
}
