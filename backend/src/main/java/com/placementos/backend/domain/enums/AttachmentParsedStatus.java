package com.placementos.backend.domain.enums;

/**
 * Parse status of an attachment file.
 * Values mirror the CHECK constraint on attachments.parsed_status in V1__initial_schema.sql.
 */
public enum AttachmentParsedStatus {
    PENDING,
    PARSED,
    FAILED,
    SKIPPED
}
