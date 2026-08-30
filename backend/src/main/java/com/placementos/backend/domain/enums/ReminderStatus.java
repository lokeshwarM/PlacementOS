package com.placementos.backend.domain.enums;

/**
 * Execution status of a scheduled reminder task.
 * Values mirror the CHECK constraint on reminder_tasks.status in V1__initial_schema.sql.
 */
public enum ReminderStatus {
    PENDING,
    COMPLETED,
    CANCELLED,
    FAILED
}
