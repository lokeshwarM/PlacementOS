package com.placementos.backend.domain.normalizer;

import java.util.*;

/**
 * Conservative specialization normalizer.
 * Maps known exact synonyms to canonical names.
 * Does NOT perform weak fuzzy matching to prevent false eligibility approvals.
 */
public final class SpecializationNormalizer {

    private static final Map<String, String> CANONICAL_MAP = new HashMap<>();

    static {
        // AI / ML
        CANONICAL_MAP.put("ai", "Artificial Intelligence");
        CANONICAL_MAP.put("aiml", "Artificial Intelligence");
        CANONICAL_MAP.put("ai & ml", "Artificial Intelligence");
        CANONICAL_MAP.put("ai and ml", "Artificial Intelligence");
        CANONICAL_MAP.put("ai/ml", "Artificial Intelligence");
        CANONICAL_MAP.put("artificial intelligence", "Artificial Intelligence");
        CANONICAL_MAP.put("artificial intelligence and machine learning", "Artificial Intelligence");

        // Data Science
        CANONICAL_MAP.put("ds", "Data Science");
        CANONICAL_MAP.put("data science", "Data Science");
        CANONICAL_MAP.put("big data", "Data Science");
        CANONICAL_MAP.put("data analytics", "Data Analytics");

        // Security
        CANONICAL_MAP.put("cyber security", "Cyber Security");
        CANONICAL_MAP.put("cybersecurity", "Cyber Security");
        CANONICAL_MAP.put("information security", "Cyber Security");
        CANONICAL_MAP.put("infosec", "Cyber Security");

        // Cloud & Networks
        CANONICAL_MAP.put("cloud computing", "Cloud Computing");
        CANONICAL_MAP.put("cloud", "Cloud Computing");
        CANONICAL_MAP.put("networking", "Networking");

        // IoT
        CANONICAL_MAP.put("iot", "IoT");
        CANONICAL_MAP.put("internet of things", "IoT");

        // Information Systems
        CANONICAL_MAP.put("information systems", "Information Systems");
        CANONICAL_MAP.put("is", "Information Systems");
    }

    private SpecializationNormalizer() {}

    /**
     * Normalizes a specialization string to its canonical name or clean capitalized form.
     */
    public static String normalize(String specialization) {
        if (specialization == null || specialization.isBlank()) {
            return null;
        }
        String clean = specialization.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[\t\n\r]+", " ")
                .replaceAll("\\s+", " ");

        return CANONICAL_MAP.getOrDefault(clean, specialization.trim());
    }

    /**
     * Checks if student specialization matches any of the allowed specializations.
     * Uses exact canonical equality only.
     */
    public static boolean matches(String studentSpec, List<String> allowedSpecs) {
        if (studentSpec == null || allowedSpecs == null || allowedSpecs.isEmpty()) {
            return false;
        }
        String normalizedStudent = normalize(studentSpec);
        if (normalizedStudent == null) {
            return false;
        }

        for (String allowed : allowedSpecs) {
            String normalizedAllowed = normalize(allowed);
            if (normalizedStudent.equalsIgnoreCase(normalizedAllowed)) {
                return true;
            }
        }
        return false;
    }
}
