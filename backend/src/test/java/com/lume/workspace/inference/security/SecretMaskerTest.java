package com.lume.workspace.inference.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SecretMasker - Unit Tests")
class SecretMaskerTest {

    @Test
    @DisplayName("should redact sensitive headers")
    void shouldRedactSensitiveHeaders() {
        Map<String, String> sanitized = SecretMasker.sanitizeHeaders(Map.of(
                "Authorization", "Bearer secret-token",
                "x-api-key", "secret",
                "Content-Type", "application/json"
        ));

        assertThat(sanitized.get("Authorization")).isEqualTo("[REDACTED]");
        assertThat(sanitized.get("x-api-key")).isEqualTo("[REDACTED]");
        assertThat(sanitized.get("Content-Type")).isEqualTo("application/json");
    }

    @Test
    @DisplayName("should sanitize error messages without leaking keys")
    void shouldSanitizeErrorMessagesWithoutLeakingKeys() {
        String sanitized = SecretMasker.sanitizeErrorMessage("Authorization: Bearer sk-12345 x-api-key: abcdef");

        assertThat(sanitized).doesNotContain("sk-12345");
        assertThat(sanitized).doesNotContain("abcdef");
        assertThat(sanitized).contains("[REDACTED]");
    }
}
