package com.placementos.backend.domain.enums;

/**
 * Deterministic matching status for a candidate in a shortlist document.
 * Values mirror the CHECK constraint on shortlist_entries.match_status.
 */
public enum ShortlistMatchStatus {
    UNMATCHED,
    MATCHED,
    AMBIGUOUS,
    REVIEW_REQUIRED
}
