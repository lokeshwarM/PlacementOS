package com.placementos.backend.domain.enums;

/**
 * Processing status for an ingested Gmail message.
 * Values mirror the CHECK constraint on processed_emails.processing_status in V1__initial_schema.sql.
 */
public enum EmailProcessingStatus {
    PENDING,
    DISCOVERED,
    QUEUED,
    RETRIEVED,
    PROCESSED,
    FAILED,
    DUPLICATE
}
