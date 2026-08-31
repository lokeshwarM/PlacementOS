package com.placementos.backend.domain.enums;

/**
 * Final overall eligibility decision for a student evaluating a placement role.
 */
public enum EligibilityDecision {
    /**
     * All applicable criteria are satisfied (PASS).
     */
    ELIGIBLE,

    /**
     * At least one applicable criterion failed (FAIL).
     */
    NOT_ELIGIBLE,

    /**
     * Cannot conclusively determine eligibility because student information is missing (UNKNOWN)
     * or an un-evaluable condition exists (UNSUPPORTED). Requires manual review.
     */
    REVIEW_REQUIRED
}
