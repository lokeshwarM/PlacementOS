package com.placementos.backend.domain.enums;

/**
 * Delivery channel for a notification.
 * Values mirror the CHECK constraint on notifications.channel in V1__initial_schema.sql.
 */
public enum NotificationChannel {
    WHATSAPP,
    TELEGRAM,
    EMAIL,
    IN_APP
}
