package com.placementos.backend.domain.enums;

/**
 * Profile completion lifecycle for a student user account.
 *
 * INCOMPLETE: Mandatory academic fields for placement eligibility evaluation are missing.
 * COMPLETE: All mandatory academic fields (name, regNo, branch, batch, cgpa, degree) are populated.
 * VERIFIED: Profile has been formally validated by college/institutional authority (not auto-assigned).
 */
public enum ProfileStatus {
    INCOMPLETE,
    COMPLETE,
    VERIFIED
}
