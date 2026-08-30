package com.placementos.backend.domain.enums;

/**
 * Application status for a student's placement drive participation.
 * Values mirror the CHECK constraint on applications.status in V1__initial_schema.sql.
 */
public enum ApplicationStatus {
    NOT_STARTED,
    ELIGIBLE,
    NOT_ELIGIBLE,
    APPLIED,
    SHORTLISTED,
    REJECTED,
    COMPLETED
}
