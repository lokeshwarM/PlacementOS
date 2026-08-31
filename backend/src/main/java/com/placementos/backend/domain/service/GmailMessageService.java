package com.placementos.backend.domain.service;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.placementos.backend.domain.dto.NormalizedAttachment;
import com.placementos.backend.domain.dto.NormalizedGmailMessage;
import com.placementos.backend.domain.entity.Attachment;
import com.placementos.backend.domain.entity.GmailMessage;
import com.placementos.backend.domain.enums.GmailMessageRetrievalStatus;
import com.placementos.backend.domain.model.GmailSource;
import com.placementos.backend.domain.repository.GmailMessageRepository;
import com.placementos.backend.domain.repository.GmailSourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

/**
 * Service responsible for retrieving full Gmail messages by message ID,
 * normalizing their MIME contents, extracting attachment metadata, and
 * durably persisting the message and its attachments in PostgreSQL.
 */
@Service
public class GmailMessageService {

    private static final Logger log = LoggerFactory.getLogger(GmailMessageService.class);

    private final GmailSourceRepository sourceRepository;
    private final GmailOAuthService oauthService;
    private final GmailMimeNormalizer mimeNormalizer;
    private final GmailMessageRepository gmailMessageRepository;
    private final ProcessedEmailService processedEmailService;
    private final org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;
    private final tools.jackson.databind.ObjectMapper objectMapper;

    public static final String REDIS_STREAM_KEY = "placementos:events:stream";

    public GmailMessageService(GmailSourceRepository sourceRepository,
                               GmailOAuthService oauthService,
                               GmailMimeNormalizer mimeNormalizer,
                               GmailMessageRepository gmailMessageRepository,
                               ProcessedEmailService processedEmailService,
                               org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate,
                               tools.jackson.databind.ObjectMapper objectMapper) {
        this.sourceRepository = sourceRepository;
        this.oauthService = oauthService;
        this.mimeNormalizer = mimeNormalizer;
        this.gmailMessageRepository = gmailMessageRepository;
        this.processedEmailService = processedEmailService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Retrieves a message from Gmail by messageId, normalizes its contents,
     * and durably persists it. Operates idempotently.
     *
     * @param sourceEmail The registered Gmail source address.
     * @param messageId   The external Gmail message ID.
     * @return The persisted or already existing {@link GmailMessage}, or null if non-retryable error.
     */
    @Transactional
    public GmailMessage retrieveAndPersistMessage(String sourceEmail, String messageId) {
        Optional<GmailSource> sourceOpt = sourceRepository.findByEmailAddress(sourceEmail);
        if (sourceOpt.isEmpty()) {
            log.warn("Cannot retrieve message {}: Gmail source not found for email {}", messageId, sourceEmail);
            processedEmailService.markFailed(messageId, "Gmail source not found: " + sourceEmail);
            return null;
        }

        GmailSource source = sourceOpt.get();

        // 1. Check idempotency guard
        Optional<GmailMessage> existing = gmailMessageRepository.findByGmailSourceIdAndMessageId(source.getId(), messageId);
        if (existing.isPresent()) {
            log.info("Gmail message {} already retrieved for source {}. Skipping duplicate retrieval.", messageId, sourceEmail);
            processedEmailService.markRetrieved(messageId);
            return existing.get();
        }

        // 2. Fetch full message from Gmail API
        Message rawMessage;
        try {
            Gmail client = oauthService.getGmailClientForSource(sourceEmail);
            rawMessage = client.users().messages().get("me", messageId)
                    .setFormat("full")
                    .execute();
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 404) {
                log.warn("Gmail message {} not found (404) in source {}. Possible deletion. Marked as FAILED.", messageId, sourceEmail);
                processedEmailService.markFailed(messageId, "Gmail API 404: Message not found");
                return null;
            } else if (e.getStatusCode() == 401) {
                log.error("Unauthorized (401) access to Gmail for source {}. Stored credential may be invalid.", sourceEmail);
                processedEmailService.markFailed(messageId, "Gmail API 401: Unauthorized");
                return null;
            } else {
                log.error("Google API error retrieving message {} for source {}: HTTP {}", messageId, sourceEmail, e.getStatusCode());
                throw new RuntimeException("Transient Gmail API error: " + e.getStatusCode(), e);
            }
        } catch (IOException e) {
            log.error("IO error retrieving message {} for source {}: {}", messageId, sourceEmail, e.getMessage());
            throw new RuntimeException("Transient IO error during Gmail message retrieval", e);
        }

        // 3. Normalize MIME contents
        NormalizedGmailMessage normalized = mimeNormalizer.normalize(rawMessage);
        if (normalized == null) {
            log.error("Failed to normalize message {} for source {}. Null payload.", messageId, sourceEmail);
            processedEmailService.markFailed(messageId, "Normalization failed: null payload");
            return null;
        }

        // 4. Build durable entity
        GmailMessage messageEntity = new GmailMessage();
        messageEntity.setMessageId(messageId);
        messageEntity.setGmailSource(source);
        messageEntity.setThreadId(normalized.getThreadId());
        messageEntity.setSubject(normalized.getSubject());
        messageEntity.setSender(normalized.getSender());
        messageEntity.setRecipients(normalized.getRecipients());
        messageEntity.setPlainTextBody(normalized.getPlainTextBody());
        messageEntity.setHtmlBody(normalized.getHtmlBody());
        messageEntity.setSnippet(normalized.getSnippet());
        messageEntity.setGmailInternalDate(normalized.getInternalDate());
        messageEntity.setRetrievalStatus(GmailMessageRetrievalStatus.RETRIEVED);
        messageEntity.setRetrievedAt(Instant.now());

        // Attachments
        if (normalized.getAttachments() != null) {
            for (NormalizedAttachment normAtt : normalized.getAttachments()) {
                Attachment attachment = new Attachment();
                attachment.setFilename(normAtt.getFilename());
                attachment.setContentType(normAtt.getContentType());
                attachment.setAttachmentId(normAtt.getAttachmentId());
                attachment.setByteSize(normAtt.getByteSize());
                messageEntity.addAttachment(attachment);
            }
        }

        // 5. Persist to PostgreSQL
        GmailMessage saved;
        try {
            saved = gmailMessageRepository.saveAndFlush(messageEntity);
        } catch (DataIntegrityViolationException e) {
            // Concurrent race condition: another thread/worker persisted it first
            log.info("Concurrent insert for message {} in source {}. Returning existing record.", messageId, sourceEmail);
            return gmailMessageRepository.findByGmailSourceIdAndMessageId(source.getId(), messageId)
                    .orElseThrow(() -> e);
        }

        // 6. Update processed_emails state
        processedEmailService.markRetrieved(messageId);

        // 7. Publish GMAIL_MESSAGE_RETRIEVED to Redis Stream for downstream processing
        try {
            com.placementos.backend.domain.event.payload.GmailMessageRetrievedPayload payload =
                    new com.placementos.backend.domain.event.payload.GmailMessageRetrievedPayload(
                            messageId,
                            saved.getThreadId(),
                            sourceEmail,
                            saved.getSubject(),
                            saved.getSender(),
                            saved.getSnippet()
                    );
            com.placementos.backend.domain.event.EventEnvelope<com.placementos.backend.domain.event.payload.GmailMessageRetrievedPayload> event =
                    new com.placementos.backend.domain.event.EventEnvelope<>(
                            com.placementos.backend.domain.event.EventType.GMAIL_MESSAGE_RETRIEVED,
                            "gmail-message-service",
                            payload
                    );

            java.util.Map<String, String> streamRecord = new java.util.HashMap<>();
            streamRecord.put("eventType", event.getEventType().name());
            streamRecord.put("eventId", event.getEventId());
            streamRecord.put("timestamp", event.getTimestamp());
            streamRecord.put("source", event.getSource());
            streamRecord.put("payload", objectMapper.writeValueAsString(payload));

            redisTemplate.opsForStream().add(
                    org.springframework.data.redis.connection.stream.StreamRecords.newRecord()
                            .in(REDIS_STREAM_KEY)
                            .ofMap(streamRecord)
            );
            log.debug("Published GMAIL_MESSAGE_RETRIEVED to stream {} for message {}", REDIS_STREAM_KEY, messageId);
        } catch (Exception e) {
            log.warn("Failed to publish GMAIL_MESSAGE_RETRIEVED to Redis Stream for message {}. Processing can continue on-demand.", messageId, e);
        }

        log.info("Successfully retrieved and persisted Gmail message {} for source {} with {} attachments.",
                messageId, sourceEmail, saved.getAttachments().size());

        return saved;
    }
}
