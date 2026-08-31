package com.placementos.backend.domain.service;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.History;
import com.google.api.services.gmail.model.HistoryMessageAdded;
import com.google.api.services.gmail.model.ListHistoryResponse;
import com.placementos.backend.domain.event.EventEnvelope;
import com.placementos.backend.domain.event.EventType;
import com.placementos.backend.domain.event.payload.GmailMessageDiscoveredPayload;
import com.placementos.backend.domain.model.GmailSource;
import com.placementos.backend.domain.repository.GmailSourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Service responsible for Gmail History API synchronization and message discovery.
 * Discovers new messages, queues them securely using idempotency checks, and
 * advances the durable cursor.
 */
@Service
public class GmailHistoryService {

    private static final Logger log = LoggerFactory.getLogger(GmailHistoryService.class);
    private static final String REDIS_EVENTS_TOPIC = "placementos.events";

    private final GmailSourceRepository sourceRepository;
    private final GmailOAuthService oauthService;
    private final ProcessedEmailService processedEmailService;
    private final RedisTemplate<String, Object> redisTemplate;

    public GmailHistoryService(GmailSourceRepository sourceRepository,
                               GmailOAuthService oauthService,
                               ProcessedEmailService processedEmailService,
                               RedisTemplate<String, Object> redisTemplate) {
        this.sourceRepository = sourceRepository;
        this.oauthService = oauthService;
        this.processedEmailService = processedEmailService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Synchronizes Gmail history for the given source mailbox.
     * Must be called synchronously to avoid advancing the Pub/Sub cursor before success.
     *
     * @param emailAddress The CDC mailbox to synchronize.
     */
    public void syncHistory(String emailAddress) {
        Optional<GmailSource> sourceOpt = sourceRepository.findByEmailAddress(emailAddress);
        if (sourceOpt.isEmpty()) {
            log.warn("Cannot sync history: source not found for email {}", emailAddress);
            return;
        }

        GmailSource source = sourceOpt.get();
        String startHistoryId = source.getLastHistoryId();

        if (startHistoryId == null || startHistoryId.isEmpty()) {
            log.info("No start history ID for {}. Cannot sync history without a baseline.", emailAddress);
            return;
        }

        Gmail client = oauthService.getGmailClientForSource(emailAddress);
        String pageToken = null;
        String latestHistoryId = null;
        int discoveredCount = 0;

        try {
            log.info("Starting history sync for {} from historyId {}", emailAddress, startHistoryId);

            do {
                ListHistoryResponse response = client.users().history().list("me")
                        .setStartHistoryId(new BigInteger(startHistoryId))
                        .setPageToken(pageToken)
                        .setHistoryTypes(List.of("messageAdded"))
                        .execute();

                if (response.getHistoryId() != null) {
                    latestHistoryId = response.getHistoryId().toString();
                }

                if (response.getHistory() != null) {
                    for (History history : response.getHistory()) {
                        if (history.getMessagesAdded() != null) {
                            for (HistoryMessageAdded added : history.getMessagesAdded()) {
                                String msgId = added.getMessage().getId();
                                String threadId = added.getMessage().getThreadId();
                                
                                boolean processed = processDiscoveredMessage(emailAddress, msgId, threadId);
                                if (processed) {
                                    discoveredCount++;
                                }
                            }
                        }
                    }
                }

                pageToken = response.getNextPageToken();
            } while (pageToken != null);

            // Successfully processed all pages. Advance the durable cursor.
            if (latestHistoryId != null && !latestHistoryId.equals(startHistoryId)) {
                source.setLastHistoryId(latestHistoryId);
                sourceRepository.save(source);
                log.info("History sync complete for {}. Discovered {} new messages. Cursor advanced to {}", 
                         emailAddress, discoveredCount, latestHistoryId);
            } else {
                log.info("History sync complete for {}. No new cursor changes.", emailAddress);
            }

        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 404) {
                // Stale or invalid history ID. Needs a full sync.
                log.error("Stale history ID {} for {}. Full sync required.", startHistoryId, emailAddress);
                source.setStatus("HISTORY_STALE");
                sourceRepository.save(source);
            } else {
                log.error("Google API error during history sync for {}: {}", emailAddress, e.getDetails().getMessage());
                throw new RuntimeException("API error during Gmail history sync", e);
            }
        } catch (IOException e) {
            log.error("IO error during history sync for {}: {}", emailAddress, e.getMessage());
            throw new RuntimeException("IO error during Gmail history sync", e);
        }
    }

    /**
     * Attempts to acquire, queue, and mark a discovered message.
     * @return true if successfully queued or already queued, false if duplicate skipped
     */
    private boolean processDiscoveredMessage(String sourceEmail, String msgId, String threadId) {
        // 1. Transaction 1: Idempotency acquisition
        boolean acquired = processedEmailService.tryAcquireDiscovery(msgId, threadId, sourceEmail, Instant.now());
        
        if (!acquired) {
            log.debug("Message {} is already processing or processed. Skipping duplicate discovery.", msgId);
            return false;
        }
        
        // 2. Publish to Redis (Outside of DB transaction for safety)
        GmailMessageDiscoveredPayload payload = new GmailMessageDiscoveredPayload(msgId, threadId, sourceEmail);
        EventEnvelope<GmailMessageDiscoveredPayload> event = new EventEnvelope<>(
                EventType.GMAIL_MESSAGE_DISCOVERED, "gmail-history-service", payload);
                
        try {
            redisTemplate.convertAndSend(REDIS_EVENTS_TOPIC, event);
            log.debug("Published GMAIL_MESSAGE_DISCOVERED for message {}", msgId);
        } catch (Exception e) {
            log.error("Failed to publish message {} to Redis. State remains DISCOVERED.", msgId, e);
            throw new RuntimeException("Redis publication failed", e);
        }

        // 3. Transaction 2: Mark successfully queued
        processedEmailService.markQueued(msgId);
        return true;
    }
}
