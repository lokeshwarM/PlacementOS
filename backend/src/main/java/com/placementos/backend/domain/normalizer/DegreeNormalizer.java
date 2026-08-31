package com.placementos.backend.domain.normalizer;

import java.util.*;

/**
 * Normalizer for academic degrees.
 * Strictly preserves distinct academic levels (e.g. B.Tech vs MCA vs M.Tech).
 */
public final class DegreeNormalizer {

    private static final Map<String, String> CANONICAL_MAP = new HashMap<>();

    static {
        // B.Tech
        CANONICAL_MAP.put("b.tech", "B.Tech");
        CANONICAL_MAP.put("btech", "B.Tech");
        CANONICAL_MAP.put("b. tech", "B.Tech");
        CANONICAL_MAP.put("bachelor of technology", "B.Tech");

        // B.E.
        CANONICAL_MAP.put("b.e.", "B.E.");
        CANONICAL_MAP.put("b.e", "B.E.");
        CANONICAL_MAP.put("be", "B.E.");
        CANONICAL_MAP.put("b. e.", "B.E.");
        CANONICAL_MAP.put("bachelor of engineering", "B.E.");

        // M.Tech
        CANONICAL_MAP.put("m.tech", "M.Tech");
        CANONICAL_MAP.put("mtech", "M.Tech");
        CANONICAL_MAP.put("m. tech", "M.Tech");
        CANONICAL_MAP.put("master of technology", "M.Tech");

        // MCA
        CANONICAL_MAP.put("mca", "MCA");
        CANONICAL_MAP.put("master of computer applications", "MCA");

        // M.S.
        CANONICAL_MAP.put("m.s.", "M.S.");
        CANONICAL_MAP.put("ms", "M.S.");
        CANONICAL_MAP.put("master of science", "M.S.");

        // B.Sc
        CANONICAL_MAP.put("b.sc", "B.Sc");
        CANONICAL_MAP.put("bsc", "B.Sc");
        CANONICAL_MAP.put("b.sc.", "B.Sc");
        CANONICAL_MAP.put("bachelor of science", "B.Sc");

        // M.Sc
        CANONICAL_MAP.put("m.sc", "M.Sc");
        CANONICAL_MAP.put("msc", "M.Sc");
        CANONICAL_MAP.put("m.sc.", "M.Sc");

        // MBA
        CANONICAL_MAP.put("mba", "MBA");
        CANONICAL_MAP.put("master of business administration", "MBA");
    }

    private DegreeNormalizer() {}

    /**
     * Normalizes a degree string to its canonical representation.
     */
    public static String normalize(String degree) {
        if (degree == null || degree.isBlank()) {
            return null;
        }
        String clean = degree.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[\t\n\r]+", " ")
                .replaceAll("\\s+", " ");

        return CANONICAL_MAP.getOrDefault(clean, degree.trim().toUpperCase(Locale.ROOT));
    }

    /**
     * Checks if student degree matches any allowed degrees.
     */
    public static boolean matches(String studentDegree, List<String> allowedDegrees) {
        if (studentDegree == null || allowedDegrees == null || allowedDegrees.isEmpty()) {
            return false;
        }
        String normalizedStudent = normalize(studentDegree);
        if (normalizedStudent == null) {
            return false;
        }

        for (String allowed : allowedDegrees) {
            String normalizedAllowed = normalize(allowed);
            if (normalizedStudent.equalsIgnoreCase(normalizedAllowed)) {
                return true;
            }
        }
        return false;
    }
}
