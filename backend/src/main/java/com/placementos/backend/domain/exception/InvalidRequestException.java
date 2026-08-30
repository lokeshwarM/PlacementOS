package com.placementos.backend.domain.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when input data violates a business rule that is not a uniqueness constraint.
 * Examples: CGPA out of range, invalid status transition.
 *
 * Maps to HTTP 400 Bad Request when surfaced through a REST controller.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }

    public static InvalidRequestException cgpaOutOfRange(double cgpa) {
        return new InvalidRequestException(
                "CGPA value " + cgpa + " is out of allowed range [0.00, 10.00].");
    }
}
