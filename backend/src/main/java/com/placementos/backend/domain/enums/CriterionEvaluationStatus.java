package com.placementos.backend.domain.enums;

/**
 * Status outcome for an individual eligibility criterion evaluation.
 */
public enum CriterionEvaluationStatus {
    /**
     * The student meets the requirement.
     */
    PASS,

    /**
     * The student fails the requirement.
     */
    FAIL,

    /**
     * The requirement cannot be evaluated because the necessary student profile data is missing/null.
     */
    UNKNOWN,

    /**
     * The condition was extracted from the placement email but cannot be evaluated automatically
     * (e.g. natural-language other conditions like 'must have driving license').
     */
    UNSUPPORTED
}
