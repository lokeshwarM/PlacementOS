package com.placementos.backend.domain.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a business-level uniqueness constraint would be violated.
 * Examples: duplicate registration number, duplicate processed email message ID,
 * duplicate application for the same student and placement drive.
 *
 * Maps to HTTP 409 Conflict when surfaced through a REST controller.
 * The underlying database constraint remains the authoritative guard.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    public static DuplicateResourceException registrationNumber(String registrationNumber) {
        return new DuplicateResourceException(
                "A student with registration number '" + registrationNumber + "' already exists.");
    }

    public static DuplicateResourceException neopatId(String neopatId) {
        return new DuplicateResourceException(
                "A student with NeoPAT ID '" + neopatId + "' already exists.");
    }

    public static DuplicateResourceException processedEmail(String messageId) {
        return new DuplicateResourceException(
                "Email message ID '" + messageId + "' has already been processed.");
    }

    public static DuplicateResourceException application(Long studentId, Long placementDriveId) {
        return new DuplicateResourceException(
                "An application already exists for student " + studentId
                + " and placement drive " + placementDriveId + ".");
    }

    public static DuplicateResourceException reminderTask(Long studentId, Long placementDriveId) {
        return new DuplicateResourceException(
                "A reminder task already exists for student " + studentId
                + " and placement drive " + placementDriveId + ".");
    }
}
