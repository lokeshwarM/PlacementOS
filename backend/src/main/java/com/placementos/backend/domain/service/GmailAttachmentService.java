package com.placementos.backend.domain.service;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.MessagePartBody;
import com.placementos.backend.domain.entity.Attachment;
import com.placementos.backend.domain.entity.GmailMessage;
import com.placementos.backend.domain.model.GmailSource;
import com.placementos.backend.domain.enums.AttachmentParsedStatus;
import com.placementos.backend.domain.event.EventEnvelope;
import com.placementos.backend.domain.event.EventType;
import com.placementos.backend.domain.exception.ResourceNotFoundException;
import com.placementos.backend.domain.repository.AttachmentRepository;
import com.placementos.backend.domain.service.storage.AttachmentStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Service responsible for retrieving attachment binaries from the Gmail API,
 * validating byte integrity, storing files via {@link AttachmentStorage},
 * and publishing ATTACHMENT_READY_FOR_PROCESSING events.
 */
@Service
public class GmailAttachmentService {

    private static final Logger log = LoggerFactory.getLogger(GmailAttachmentService.class);
    private static final String STREAM_KEY = "placementos:events:stream";

    private final AttachmentRepository attachmentRepository;
    private final AttachmentStorage attachmentStorage;
    private final GmailOAuthService gmailOAuthService;
    private final RedisTemplate<String, Object> redisTemplate;

    public GmailAttachmentService(AttachmentRepository attachmentRepository,
                                  AttachmentStorage attachmentStorage,
                                  GmailOAuthService gmailOAuthService,
                                  RedisTemplate<String, Object> redisTemplate) {
        this.attachmentRepository = attachmentRepository;
        this.attachmentStorage = attachmentStorage;
        this.gmailOAuthService = gmailOAuthService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Downloads and persists the binary content for a recorded email attachment.
     */
    @Transactional
    public Attachment downloadAndStoreAttachment(Long attachmentRecordId) {
        Attachment attachment = attachmentRepository.findById(attachmentRecordId)
                .orElseThrow(() -> ResourceNotFoundException.attachment(attachmentRecordId));

        // 1. Idempotency check: if already downloaded and file exists in storage, return directly
        if (attachment.getStorageReference() != null && attachmentStorage.exists(attachment.getStorageReference())) {
            log.info("Attachment id={} is already downloaded at reference: {}", attachmentRecordId, attachment.getStorageReference());
            return attachment;
        }

        GmailMessage gmailMessage = attachment.getGmailMessage();
        if (gmailMessage == null) {
            log.warn("Cannot download attachment id={}: no associated GmailMessage record", attachmentRecordId);
            attachment.setParsedStatus(AttachmentParsedStatus.FAILED);
            return attachmentRepository.save(attachment);
        }

        GmailSource source = gmailMessage.getGmailSource();
        if (source == null) {
            log.warn("Cannot download attachment id={}: no associated GmailSource", attachmentRecordId);
            attachment.setParsedStatus(AttachmentParsedStatus.FAILED);
            return attachmentRepository.save(attachment);
        }

        if (attachment.getAttachmentId() == null || attachment.getAttachmentId().isBlank()) {
            log.warn("Cannot download attachment id={}: no Gmail attachmentId", attachmentRecordId);
            attachment.setParsedStatus(AttachmentParsedStatus.FAILED);
            return attachmentRepository.save(attachment);
        }

        try {
            // 2. Fetch binary bytes from Gmail API
            Gmail gmailClient = gmailOAuthService.getGmailClientForSource(source.getEmailAddress());
            MessagePartBody partBody = gmailClient.users().messages().attachments()
                    .get("me", gmailMessage.getMessageId(), attachment.getAttachmentId())
                    .execute();

            if (partBody == null || partBody.getData() == null) {
                log.error("Empty attachment body received from Gmail for attachment id={}", attachmentRecordId);
                attachment.setParsedStatus(AttachmentParsedStatus.FAILED);
                return attachmentRepository.save(attachment);
            }

            byte[] binaryData = Base64.getUrlDecoder().decode(partBody.getData());
            String checksum = calculateSha256(binaryData);

            // 3. Generate safe storage key and store binary via AttachmentStorage abstraction
            String safeFilename = sanitizeFilename(attachment.getFilename());
            String storageKey = String.format("attachments/%d/%s_%s",
                    gmailMessage.getId(),
                    UUID.randomUUID().toString().substring(0, 8),
                    safeFilename);

            String storageRef = attachmentStorage.store(storageKey, binaryData, attachment.getContentType());

            // 4. Update database metadata
            attachment.setStorageReference(storageRef);
            attachment.setByteSize((long) binaryData.length);
            attachment.setSha256Checksum(checksum);
            attachment.setParsedStatus(AttachmentParsedStatus.DOWNLOADED);
            Attachment saved = attachmentRepository.save(attachment);

            log.info("Successfully downloaded and stored attachment id={} (filename={}, size={} bytes, checksum={})",
                    saved.getId(), saved.getFilename(), binaryData.length, checksum);

            // 5. Emit ATTACHMENT_READY_FOR_PROCESSING event to Redis Streams
            publishAttachmentReadyEvent(saved, gmailMessage);

            return saved;
        } catch (IOException e) {
            log.error("IO error downloading attachment id={} for message {}: {}",
                    attachmentRecordId, gmailMessage.getMessageId(), e.getMessage());
            attachment.setParsedStatus(AttachmentParsedStatus.FAILED);
            return attachmentRepository.save(attachment);
        }
    }

    /**
     * Retrieves the binary payload for an attachment record.
     */
    public byte[] getAttachmentContent(Long attachmentRecordId) throws IOException {
        Attachment attachment = attachmentRepository.findById(attachmentRecordId)
                .orElseThrow(() -> ResourceNotFoundException.attachment(attachmentRecordId));

        if (attachment.getStorageReference() == null) {
            throw new IOException("Attachment binary has not been downloaded yet (id=" + attachmentRecordId + ")");
        }

        return attachmentStorage.load(attachment.getStorageReference());
    }

    private void publishAttachmentReadyEvent(Attachment attachment, GmailMessage gmailMessage) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("attachmentId", attachment.getId());
            payload.put("gmailMessageRecordId", gmailMessage.getId());
            payload.put("gmailMessageId", gmailMessage.getMessageId());
            payload.put("filename", attachment.getFilename());
            payload.put("contentType", attachment.getContentType());
            payload.put("byteSize", attachment.getByteSize());
            payload.put("storageReference", attachment.getStorageReference());
            payload.put("sha256Checksum", attachment.getSha256Checksum());

            EventEnvelope<Map<String, Object>> envelope = new EventEnvelope<>(
                    EventType.ATTACHMENT_READY_FOR_PROCESSING,
                    "GmailAttachmentService",
                    payload
            );

            Map<String, Object> streamMap = new HashMap<>();
            streamMap.put("eventId", envelope.getEventId());
            streamMap.put("eventType", envelope.getEventType().name());
            streamMap.put("timestamp", envelope.getTimestamp().toString());
            streamMap.put("source", envelope.getSource());
            streamMap.put("attachmentId", String.valueOf(attachment.getId()));
            streamMap.put("filename", attachment.getFilename());
            streamMap.put("storageReference", attachment.getStorageReference());

            RecordId recordId = redisTemplate.opsForStream().add(
                    StreamRecords.newRecord().in(STREAM_KEY).ofMap(streamMap)
            );
            log.info("Published ATTACHMENT_READY_FOR_PROCESSING event {} for attachment id={}", recordId, attachment.getId());
        } catch (Exception e) {
            log.error("Failed to publish ATTACHMENT_READY_FOR_PROCESSING event for attachment id={}: {}",
                    attachment.getId(), e.getMessage());
        }
    }

    private String calculateSha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "attachment.bin";
        }
        // Remove path elements and special control characters
        String clean = filename.replaceAll("[/\\\\:*?\"<>|]", "_").trim();
        return clean.isEmpty() ? "attachment.bin" : clean;
    }
}
