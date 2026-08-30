package com.placementos.backend.domain.enums;

/**
 * Delivery status of a notification.
 * Values mirror the CHECK constraint on notifications.status in V1__initial_schema.sql.
 */
public enum NotificationStatus {
    PENDING,
    SENT,
    FAILED,
    SKIPPED
}
