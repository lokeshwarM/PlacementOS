package com.placementos.backend.domain.enums;

/**
 * Lifecycle status of a placement drive.
 * Values mirror the CHECK constraint on placement_drives.status in V1__initial_schema.sql.
 */
public enum DriveStatus {
    OPEN,
    CLOSED,
    CANCELLED,
    COMPLETED
}
