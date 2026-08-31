package com.placementos.backend.domain.event;

public enum EventType {
    PLACEMENT_EMAIL_RECEIVED,
    DOCUMENT_PROCESS_REQUESTED,
    /**
     * Signals that a Gmail push notification arrived and the mailbox cursor has
     * been advanced. This event type is reserved for the future History API
     * synchronization milestone. It is NOT published in the current milestone.
     */
    GMAIL_MAILBOX_CHANGED,
    
    /**
     * Signals that a new incoming placement email has been discovered via the Gmail History API.
     * Sent to Redis to queue the email for processing.
     */
    GMAIL_MESSAGE_DISCOVERED,

    /**
     * Signals that a raw Gmail message has been retrieved, MIME-normalized, and durably persisted.
     * Triggers downstream classification and extraction processing.
     */
    GMAIL_MESSAGE_RETRIEVED,

    /**
     * Signals that a placement email has been classified and its structured information extracted.
     */
    PLACEMENT_EXTRACTED
}
