package com.placementos.backend.domain.normalizer;

import java.util.*;

/**
 * Centralized, conservative branch normalization and matching component.
 * Maps known synonym branches to canonical acronym codes without over-normalizing distinct branches.
 */
public final class BranchNormalizer {

    private static final Map<String, String> CANONICAL_MAP = new HashMap<>();

    static {
        // Computer Science
        CANONICAL_MAP.put("cse", "CSE");
        CANONICAL_MAP.put("cs", "CSE");
        CANONICAL_MAP.put("computer science", "CSE");
        CANONICAL_MAP.put("computer science and engineering", "CSE");
        CANONICAL_MAP.put("computer science & engineering", "CSE");
        CANONICAL_MAP.put("b.tech cse", "CSE");
        CANONICAL_MAP.put("btech cse", "CSE");

        // Information Technology
        CANONICAL_MAP.put("it", "IT");
        CANONICAL_MAP.put("information technology", "IT");
        CANONICAL_MAP.put("b.tech it", "IT");
        CANONICAL_MAP.put("btech it", "IT");

        // Electronics and Communication
        CANONICAL_MAP.put("ece", "ECE");
        CANONICAL_MAP.put("electronics and communication", "ECE");
        CANONICAL_MAP.put("electronics and communication engineering", "ECE");
        CANONICAL_MAP.put("electronics & communication engineering", "ECE");
        CANONICAL_MAP.put("b.tech ece", "ECE");
        CANONICAL_MAP.put("btech ece", "ECE");

        // Electrical and Electronics
        CANONICAL_MAP.put("eee", "EEE");
        CANONICAL_MAP.put("electrical and electronics", "EEE");
        CANONICAL_MAP.put("electrical and electronics engineering", "EEE");
        CANONICAL_MAP.put("electrical & electronics engineering", "EEE");
        CANONICAL_MAP.put("b.tech eee", "EEE");
        CANONICAL_MAP.put("btech eee", "EEE");

        // Mechanical
        CANONICAL_MAP.put("mech", "MECH");
        CANONICAL_MAP.put("mechanical", "MECH");
        CANONICAL_MAP.put("mechanical engineering", "MECH");
        CANONICAL_MAP.put("b.tech mech", "MECH");
        CANONICAL_MAP.put("btech mech", "MECH");

        // Civil
        CANONICAL_MAP.put("civil", "CIVIL");
        CANONICAL_MAP.put("civil engineering", "CIVIL");
        CANONICAL_MAP.put("b.tech civil", "CIVIL");
        CANONICAL_MAP.put("btech civil", "CIVIL");

        // AI & DS
        CANONICAL_MAP.put("aids", "AI&DS");
        CANONICAL_MAP.put("ai&ds", "AI&DS");
        CANONICAL_MAP.put("ai & ds", "AI&DS");
        CANONICAL_MAP.put("ai and ds", "AI&DS");
        CANONICAL_MAP.put("artificial intelligence and data science", "AI&DS");

        // CSBS
        CANONICAL_MAP.put("csbs", "CSBS");
        CANONICAL_MAP.put("computer science and business systems", "CSBS");
    }

    private BranchNormalizer() {}

    /**
     * Normalizes a branch string into its canonical code or clean uppercase form.
     */
    public static String normalize(String branch) {
        if (branch == null || branch.isBlank()) {
            return null;
        }
        String clean = branch.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[\t\n\r]+", " ")
                .replaceAll("\\s+", " ");

        return CANONICAL_MAP.getOrDefault(clean, branch.trim().toUpperCase(Locale.ROOT));
    }

    /**
     * Checks if the student's branch matches any of the allowed branches.
     */
    public static boolean matches(String studentBranch, List<String> allowedBranches) {
        if (studentBranch == null || allowedBranches == null || allowedBranches.isEmpty()) {
            return false;
        }
        String normalizedStudent = normalize(studentBranch);
        if (normalizedStudent == null) {
            return false;
        }

        for (String allowed : allowedBranches) {
            String normalizedAllowed = normalize(allowed);
            if (normalizedStudent.equalsIgnoreCase(normalizedAllowed)) {
                return true;
            }
        }
        return false;
    }
}
