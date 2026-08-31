package com.placementos.backend.domain.service;

import com.placementos.backend.domain.entity.ProcessedEmail;
import com.placementos.backend.domain.enums.EmailProcessingStatus;
import com.placementos.backend.domain.exception.DuplicateResourceException;
import com.placementos.backend.domain.repository.ProcessedEmailRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;

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
     * Idempotently acquires the right to process a newly discovered message.
     * Returns true if the caller should proceed to publish the event to Redis.
     * Returns false if the message has already been successfully queued or processed.
     */
    @Transactional
    public boolean tryAcquireDiscovery(String messageId, String threadId, String sourceIdentifier, Instant receivedAt) {
        Optional<ProcessedEmail> existing = processedEmailRepository.findByMessageId(messageId);
        if (existing.isPresent()) {
            // If it is DISCOVERED, a previous attempt failed before publishing to Redis.
            // We return true to allow the retry. Any other state (QUEUED, PROCESSED) means
            // we already handed it off successfully.
            return existing.get().getProcessingStatus() == EmailProcessingStatus.DISCOVERED;
        }

        ProcessedEmail entry = new ProcessedEmail();
        entry.setMessageId(messageId);
        entry.setThreadId(threadId);
        entry.setSourceIdentifier(sourceIdentifier);
        entry.setReceivedAt(receivedAt);
        entry.setProcessingStatus(EmailProcessingStatus.DISCOVERED);

        try {
            processedEmailRepository.saveAndFlush(entry);
            return true;
        } catch (DataIntegrityViolationException e) {
            // Concurrent ingestion race condition: another thread inserted it first.
            return false;
        }
    }

    /**
     * Marks an email as successfully queued to Redis.
     */
    @Transactional
    public void markQueued(String messageId) {
        ProcessedEmail entry = processedEmailRepository.findByMessageId(messageId)
                .orElseThrow(() -> new IllegalStateException("Cannot mark queued: message ID not found - " + messageId));
        entry.setProcessingStatus(EmailProcessingStatus.QUEUED);
        processedEmailRepository.save(entry);
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
