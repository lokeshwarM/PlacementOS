package com.placementos.backend.domain.enums;

/**
 * Type of notification sent to a student.
 * Values mirror the CHECK constraint on notifications.notification_type in V1__initial_schema.sql.
 */
public enum NotificationType {
    ELIGIBILITY,
    SHORTLIST,
    DEADLINE,
    REMINDER
}
