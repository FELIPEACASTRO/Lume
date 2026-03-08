package com.lume.workspace.inference.security;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class SecretMasker {

    private SecretMasker() {
    }

    public static String maskValue(String value) {
        if (value == null || value.isBlank()) {
            return "[ausente]";
        }
        if (value.length() <= 6) {
            return "***";
        }
        return value.substring(0, 2) + "***" + value.substring(value.length() - 2);
    }

    public static Map<String, String> sanitizeHeaders(Map<String, String> headers) {
        Map<String, String> sanitized = new LinkedHashMap<>();
        headers.forEach((key, value) -> sanitized.put(key, shouldMaskHeader(key) ? "[REDACTED]" : value));
        return sanitized;
    }

    public static String sanitizeErrorMessage(String message) {
        if (message == null || message.isBlank()) {
            return message;
        }
        return message
                .replaceAll("(?i)bearer\\s+[a-z0-9_\\-\\.]+", "Bearer [REDACTED]")
                .replaceAll("(?i)x-api-key[:=]\\s*[a-z0-9_\\-\\.]+", "x-api-key=[REDACTED]")
                .replaceAll("(?i)x-goog-api-key[:=]\\s*[a-z0-9_\\-\\.]+", "x-goog-api-key=[REDACTED]");
    }

    private static boolean shouldMaskHeader(String key) {
        String normalized = key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("authorization")
                || normalized.equals("x-api-key")
                || normalized.equals("x-goog-api-key")
                || normalized.equals("xi-api-key");
    }
}
