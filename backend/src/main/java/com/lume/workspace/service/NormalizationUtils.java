package com.lume.workspace.service;

import java.util.Locale;

/**
 * Shared normalization utilities for workspace services.
 */
public final class NormalizationUtils {

    private NormalizationUtils() {
    }

    /**
     * Normalizes a value to a lowercase, underscore-separated token.
     * Trims whitespace, lowercases, and replaces dashes and spaces with underscores.
     */
    public static String token(String value) {
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
    }
}
