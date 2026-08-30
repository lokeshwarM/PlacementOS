package com.placementos.backend.domain.entity;

import com.placementos.backend.domain.enums.EmailProcessingStatus;
import jakarta.persistence.*;
import java.time.Instant;

/**
 * JPA entity for the {@code processed_emails} table.
 * Schema is managed by Flyway; this class is a mapping layer only.
 *
 * Purpose: idempotency guard for Gmail ingestion.
 * message_id is the Gmail Message-ID; it must be UNIQUE.
 *
 * Timestamps are TIMESTAMPTZ → Instant (UTC).
 */
@Entity
@Table(
    name = "processed_emails",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_processed_emails_message_id", columnNames = "message_id")
    }
)
public class ProcessedEmail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false, length = 255)
    private String messageId;

    @Column(name = "thread_id", length = 255)
    private String threadId;

    @Column(name = "source_identifier", length = 255)
    private String sourceIdentifier;

    @Column(name = "received_at")
    private Instant receivedAt;

    @Column(name = "processed_at", nullable = false,
            columnDefinition = "TIMESTAMPTZ DEFAULT NOW()")
    private Instant processedAt;

    /**
     * Processing status. Stored as VARCHAR(50) with CHECK constraint.
     * @see EmailProcessingStatus for valid values.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 50)
    private EmailProcessingStatus processingStatus = EmailProcessingStatus.PENDING;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // -------------------------------------------------------------------------
    // Lifecycle hooks
    // -------------------------------------------------------------------------
    @PrePersist
    protected void onCreate() {
        if (processedAt == null) processedAt = Instant.now();
        if (processingStatus == null) processingStatus = EmailProcessingStatus.PENDING;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------
    protected ProcessedEmail() {}

    // -------------------------------------------------------------------------
    // Getters and Setters
    // -------------------------------------------------------------------------
    public Long getId() { return id; }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getThreadId() { return threadId; }
    public void setThreadId(String threadId) { this.threadId = threadId; }

    public String getSourceIdentifier() { return sourceIdentifier; }
    public void setSourceIdentifier(String sourceIdentifier) { this.sourceIdentifier = sourceIdentifier; }

    public Instant getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Instant receivedAt) { this.receivedAt = receivedAt; }

    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }

    public EmailProcessingStatus getProcessingStatus() { return processingStatus; }
    public void setProcessingStatus(EmailProcessingStatus processingStatus) { this.processingStatus = processingStatus; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    // -------------------------------------------------------------------------
    // equals / hashCode
    // -------------------------------------------------------------------------
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProcessedEmail)) return false;
        ProcessedEmail other = (ProcessedEmail) o;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "ProcessedEmail{id=" + id + ", messageId='" + messageId + "', processingStatus=" + processingStatus + "}";
    }
}
