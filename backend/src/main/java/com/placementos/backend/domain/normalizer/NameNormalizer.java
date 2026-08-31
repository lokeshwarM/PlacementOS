package com.placementos.backend.domain.normalizer;

import java.util.Locale;

/**
 * Deterministic student candidate name normalizer.
 * Provides safe, conservative normalization across casing, excess whitespace, and punctuation.
 * Does NOT perform unsafe fuzzy matching.
 */
public final class NameNormalizer {

    private NameNormalizer() {}

    /**
     * Normalizes a candidate or student name:
     * - Trims leading/trailing whitespace
     * - Collapses internal whitespace
     * - Converts to UPPERCASE
     * - Strips honorifics (Mr., Ms., Mrs.)
     * - Normalizes periods and punctuation
     */
    public static String normalize(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }

        String cleaned = name.trim();

        // Remove common honorific prefixes
        cleaned = cleaned.replaceAll("(?i)^(mr\\.?|ms\\.?|mrs\\.?|dr\\.?)\\s+", "");

        // Remove punctuation except standard alphanumeric and whitespace
        cleaned = cleaned.replaceAll("[^a-zA-Z0-9\\s]", " ");

        // Collapse multiple spaces into single space and convert to uppercase
        cleaned = cleaned.replaceAll("\\s+", " ").trim().toUpperCase(Locale.ROOT);

        return cleaned;
    }

    /**
     * Checks if two names match exactly under conservative normalization.
     */
    public static boolean matches(String name1, String name2) {
        String n1 = normalize(name1);
        String n2 = normalize(name2);
        if (n1.isEmpty() || n2.isEmpty()) {
            return false;
        }
        return n1.equals(n2);
    }
}
