package com.placementos.backend.domain.service;

import com.placementos.backend.domain.entity.ProcessedEmail;
import com.placementos.backend.domain.enums.EmailProcessingStatus;
import com.placementos.backend.domain.exception.DuplicateResourceException;
import com.placementos.backend.domain.repository.ProcessedEmailRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Business service for Gmail ingestion idempotency.
 *
 * This service owns the idempotency boundary for email processing.
 * The future Gmail integration component must call {@link #isDuplicate(String)}
 * before beginning any processing, and must call {@link #registerProcessed(String, String, String, Instant)}
 * to record successful ingestion.
 *
 * The database UNIQUE constraint on processed_emails.message_id is the final
 * guard against concurrent duplicate ingestion; this service provides the
 * business-level first-check for clean error handling.
 *
 * No Gmail API calls are made here.
 */
@Service
@Transactional(readOnly = true)
public class ProcessedEmailService {

    private final ProcessedEmailRepository processedEmailRepository;

    public ProcessedEmailService(ProcessedEmailRepository processedEmailRepository) {
        this.processedEmailRepository = processedEmailRepository;
    }

    /**
     * Returns true if the given Gmail message ID has already been processed.
     * Must be called before starting any processing of an incoming email.
     */
    public boolean isDuplicate(String messageId) {
        return processedEmailRepository.existsByMessageId(messageId);
    }

    /**
     * Registers a successfully processed Gmail message.
     * Throws {@link DuplicateResourceException} if the message ID already exists,
     * which indicates a race condition or logic error.
     */
    @Transactional
    public ProcessedEmail registerProcessed(String messageId,
                                            String threadId,
                                            String sourceIdentifier,
                                            Instant receivedAt) {
        if (processedEmailRepository.existsByMessageId(messageId)) {
            throw DuplicateResourceException.processedEmail(messageId);
        }

        ProcessedEmail entry = new ProcessedEmail();
        entry.setMessageId(messageId);
        entry.setThreadId(threadId);
        entry.setSourceIdentifier(sourceIdentifier);
        entry.setReceivedAt(receivedAt);
        entry.setProcessingStatus(EmailProcessingStatus.PROCESSED);

        return processedEmailRepository.save(entry);
    }

    /**
     * Registers a failed processing attempt so that the failure is tracked
     * and not silently lost.
     */
    @Transactional
    public ProcessedEmail registerFailed(String messageId,
                                         String threadId,
                                         String sourceIdentifier,
                                         Instant receivedAt,
                                         String errorMessage) {
        ProcessedEmail entry = new ProcessedEmail();
        entry.setMessageId(messageId);
        entry.setThreadId(threadId);
        entry.setSourceIdentifier(sourceIdentifier);
        entry.setReceivedAt(receivedAt);
        entry.setProcessingStatus(EmailProcessingStatus.FAILED);
        entry.setErrorMessage(errorMessage);

        return processedEmailRepository.save(entry);
    }

    /**
     * Retrieves the processing record for a message, if it exists.
     */
    public Optional<ProcessedEmail> findByMessageId(String messageId) {
        return processedEmailRepository.findByMessageId(messageId);
    }
}
