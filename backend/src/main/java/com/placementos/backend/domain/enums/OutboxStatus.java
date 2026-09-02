package com.placementos.backend.domain.enums;

/**
 * Lifecycle states for transactional outbox records.
 */
public enum OutboxStatus {
    PENDING,
    PROCESSING,
    SENT,
    FAILED,
    RETRYING,
    CANCELLED
}
