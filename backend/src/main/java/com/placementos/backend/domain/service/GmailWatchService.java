package com.placementos.backend.domain.service;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.WatchRequest;
import com.google.api.services.gmail.model.WatchResponse;
import com.placementos.backend.domain.model.GmailSource;
import com.placementos.backend.domain.repository.GmailSourceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;

/**
 * Manages Gmail push watch registrations for authorized CDC source mailboxes.
 *
 * <p>A Gmail watch instructs Google to publish a Pub/Sub notification whenever
 * the monitored mailbox changes. The watch itself contains only a mailbox cursor
 * (historyId) and an expiration timestamp — it does NOT deliver email content.
 *
 * <p><b>Scope boundary:</b> This service registers watches and tracks their
 * lifecycle state. Actual Gmail History API synchronization (fetching the changes
 * that occurred since the last known historyId) is deferred to the next milestone.
 *
 * <p><b>Watch expiration:</b> Gmail watch registrations expire within approximately
 * 7 days (604800 seconds). A scheduled renewal workflow — using {@link #renewWatch(String)}
 * — must be implemented in a future milestone. No {@code @Scheduled} annotation is
 * used here.
 */
@Service
public class GmailWatchService {

    private final GmailOAuthService gmailOAuthService;
    private final GmailSourceRepository gmailSourceRepository;

    @Value("${app.google.pubsub.topic}")
    private String pubSubTopic;

    public GmailWatchService(GmailOAuthService gmailOAuthService,
                             GmailSourceRepository gmailSourceRepository) {
        this.gmailOAuthService = gmailOAuthService;
        this.gmailSourceRepository = gmailSourceRepository;
    }

    /**
     * Registers a Gmail push watch for the given source mailbox.
     *
     * <p>On success, persists the returned {@code historyId} (as an opaque String cursor
     * per the Gmail API contract) and the watch expiration to the {@code gmail_sources} row.
     * The stored {@code historyId} is a synchronization cursor only — no Gmail messages
     * are fetched or processed by this call.
     *
     * @param emailAddress the authorized Gmail source email address
     * @return the updated {@link GmailSource}
     * @throws IOException              if the Gmail API call fails
     * @throws IllegalArgumentException if no GmailSource is registered for the email
     */
    @Transactional
    public GmailSource registerWatch(String emailAddress) throws IOException {
        Gmail gmail = gmailOAuthService.getGmailClientForSource(emailAddress);

        WatchRequest watchRequest = new WatchRequest()
                .setTopicName(pubSubTopic)
                .setLabelIds(Collections.singletonList("INBOX"));

        WatchResponse response = gmail.users().watch("me", watchRequest).execute();

        return persistWatchState(emailAddress, response);
    }

    /**
     * Renews an existing Gmail push watch for the given source mailbox.
     *
     * <p>Functionally identical to {@link #registerWatch(String)}: Google's watch API
     * is idempotent — calling watch again replaces the previous registration and returns
     * a fresh expiration. This method exists as a named boundary for the future scheduled
     * renewal workflow.
     *
     * <p><b>Note:</b> Scheduled invocation is deferred to a future milestone.
     *
     * @param emailAddress the authorized Gmail source email address
     * @return the updated {@link GmailSource}
     * @throws IOException if the Gmail API call fails
     */
    @Transactional
    public GmailSource renewWatch(String emailAddress) throws IOException {
        // Google's watch API is idempotent: re-registering replaces the previous watch
        return registerWatch(emailAddress);
    }

    private GmailSource persistWatchState(String emailAddress, WatchResponse response) {
        GmailSource source = gmailSourceRepository.findByEmailAddress(emailAddress)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No Gmail source registered for: " + emailAddress));

        // historyId is stored as a String to match the Gmail API contract.
        // The Java client library represents it internally as BigInteger,
        // but we store it as a String to remain agnostic of numeric semantics.
        // This is a cursor position — it does NOT represent processed messages.
        source.setLastHistoryId(response.getHistoryId() != null
                ? response.getHistoryId().toString() : null);

        // WatchResponse.getExpiration() is epoch milliseconds as a Long
        if (response.getExpiration() != null) {
            ZonedDateTime expiration = Instant.ofEpochMilli(response.getExpiration())
                    .atZone(ZoneOffset.UTC);
            source.setWatchExpiration(expiration);
        }

        source.setWatchStatus("ACTIVE");

        return gmailSourceRepository.save(source);
    }
}
