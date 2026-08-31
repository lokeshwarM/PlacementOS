package com.placementos.backend.domain.service;

import com.placementos.backend.domain.model.GmailSource;
import com.placementos.backend.domain.repository.GmailSourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Handles decoded Gmail push notifications by advancing the mailbox history cursor.
 *
 * <h2>What this service does</h2>
 * <p>When Google Pub/Sub delivers a Gmail push notification, the notification contains:
 * <ul>
 *   <li>{@code emailAddress} — the monitored mailbox</li>
 *   <li>{@code historyId} — an opaque string cursor identifying the position in the
 *       mailbox's change history at the time Google sent the notification</li>
 * </ul>
 * <p>This service advances the durable {@code last_history_id} cursor stored in the
 * {@code gmail_sources} table when the incoming cursor is strictly newer (lexicographically
 * greater, as per Gmail's monotonically increasing historyId guarantee).
 *
 * <h2>What this service does NOT do</h2>
 * <p>Advancing the cursor here does <strong>not</strong> mean Gmail messages have been
 * fetched or processed. The incoming historyId simply tells us that something changed
 * in the mailbox at or around that position. Actual Gmail History API synchronization —
 * calling {@code gmail.users().history().list(startHistoryId)} to retrieve what changed —
 * is deferred to the <strong>next milestone</strong>.
 *
 * <h2>Idempotency &amp; out-of-order delivery</h2>
 * <p>Pub/Sub may redeliver the same notification (at-least-once delivery). If the
 * incoming historyId is equal to or less than the stored cursor, this method is a no-op
 * and returns {@code HandlerResult.ALREADY_SEEN}. The controller must still return HTTP
 * 2xx to acknowledge the duplicate so Pub/Sub stops redelivering it.
 *
 * <h2>Initial watch notification</h2>
 * <p>When a Gmail watch is first registered, Google sends an initial synthetic notification.
 * This notification signals watch establishment — not that a new placement email arrived.
 * This handler treats it identically to any other notification: the cursor advances if the
 * incoming historyId is newer than the stored one. No email content is fetched regardless.
 *
 * <h2>Identifier discipline</h2>
 * <ul>
 *   <li><b>Pub/Sub messageId</b> — identifies a Pub/Sub delivery (lives at the queue layer)</li>
 *   <li><b>Gmail historyId</b> — identifies a position in the mailbox change log (this field)</li>
 *   <li><b>Gmail messageId</b> — identifies an individual email (not present in push notifications)</li>
 * </ul>
 * <p>These three identifiers are not interchangeable and must never be stored in each other's fields.
 */
@Service
public class GmailNotificationHandler {

    private final GmailSourceRepository gmailSourceRepository;

    public GmailNotificationHandler(GmailSourceRepository gmailSourceRepository) {
        this.gmailSourceRepository = gmailSourceRepository;
    }

    public enum HandlerResult {
        /** Cursor advanced successfully — the incoming historyId was newer than the stored one. */
        CURSOR_ADVANCED,
        /** Incoming historyId was not newer than the stored cursor — no-op, safe to acknowledge. */
        ALREADY_SEEN,
        /** No GmailSource is registered for the email address in the notification. */
        SOURCE_NOT_FOUND
    }

    /**
     * Processes a decoded Gmail push notification by advancing the history cursor if appropriate.
     *
     * @param emailAddress      the mailbox address from the notification payload
     * @param incomingHistoryId the historyId string from the notification payload (opaque cursor)
     * @return a {@link HandlerResult} indicating what action was taken
     */
    @Transactional
    public HandlerResult handle(String emailAddress, String incomingHistoryId) {
        Optional<GmailSource> sourceOpt = gmailSourceRepository.findByEmailAddress(emailAddress);

        if (sourceOpt.isEmpty()) {
            return HandlerResult.SOURCE_NOT_FOUND;
        }

        GmailSource source = sourceOpt.get();
        String storedHistoryId = source.getLastHistoryId();

        // historyId values are monotonically increasing unsigned integers represented
        // as strings. We compare them numerically to handle lexicographic edge cases
        // (e.g. "9" vs "10" — string comparison would give the wrong order).
        if (storedHistoryId != null && !isNewer(incomingHistoryId, storedHistoryId)) {
            // Equal or older — idempotent no-op. Acknowledge safely.
            return HandlerResult.ALREADY_SEEN;
        }

        // Advance the cursor. This does NOT mean messages have been processed.
        // The next milestone will use this cursor as startHistoryId for History API calls.
        source.setLastHistoryId(incomingHistoryId);
        gmailSourceRepository.save(source);

        return HandlerResult.CURSOR_ADVANCED;
    }

    /**
     * Returns true if {@code incoming} is strictly greater than {@code stored}.
     * Falls back to string comparison if either value is not a valid long.
     */
    private boolean isNewer(String incoming, String stored) {
        try {
            return Long.parseUnsignedLong(incoming) > Long.parseUnsignedLong(stored);
        } catch (NumberFormatException e) {
            // Defensive fallback: treat as opaque strings
            return incoming.compareTo(stored) > 0;
        }
    }
}
