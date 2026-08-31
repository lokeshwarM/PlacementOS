package com.placementos.backend.domain.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a requested resource does not exist in the database.
 * Maps to HTTP 404 when surfaced through a REST controller.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException student(Long id) {
        return new ResourceNotFoundException("Student not found with id: " + id);
    }

    public static ResourceNotFoundException studentByRegistration(String regNumber) {
        return new ResourceNotFoundException("Student not found with registration number: " + regNumber);
    }

    public static ResourceNotFoundException placementDrive(Long id) {
        return new ResourceNotFoundException("Placement drive not found with id: " + id);
    }

    public static ResourceNotFoundException attachment(Long id) {
        return new ResourceNotFoundException("Attachment not found with id: " + id);
    }

    public static ResourceNotFoundException application(Long id) {
        return new ResourceNotFoundException("Application not found with id: " + id);
    }

    public static ResourceNotFoundException placementRole(Long id) {
        return new ResourceNotFoundException("Placement role not found with id: " + id);
    }
}
