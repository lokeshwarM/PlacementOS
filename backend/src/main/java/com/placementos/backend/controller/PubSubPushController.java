package com.placementos.backend.controller;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.databind.ObjectMapper;
import com.placementos.backend.config.PubSubJwtValidator;
import com.placementos.backend.domain.service.GmailNotificationHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;
import java.util.Map;

/**
 * Receives authenticated Google Cloud Pub/Sub push notifications for Gmail mailbox changes.
 *
 * <h2>Authentication</h2>
 * <p>This endpoint uses Google's authenticated push subscription model. Every Pub/Sub push
 * delivery carries an {@code Authorization: Bearer <signed-jwt>} header containing a
 * Google-signed JWT. The JWT is validated by {@link PubSubJwtValidator} before the request
 * body is examined. Requests without a valid JWT are rejected with HTTP 401.
 *
 * <p><b>This is distinct from the OAuth2 authorize/callback flow</b> used for Gmail source
 * registration. OAuth2 authorization uses CSRF state and authorization-code exchange.
 * Pub/Sub push authentication uses Google-signed JWTs tied to the push subscription's
 * service account. These two mechanisms are completely separate.
 *
 * <h2>Pub/Sub message envelope</h2>
 * <pre>{@code
 * {
 *   "message": {
 *     "data": "<base64-encoded Gmail notification JSON>",
 *     "messageId": "<Pub/Sub delivery ID>",
 *     "publishTime": "..."
 *   },
 *   "subscription": "projects/.../subscriptions/..."
 * }
 * }</pre>
 *
 * <p>The {@code message.data} field is Base64-decoded to obtain the Gmail notification:
 * <pre>{@code { "emailAddress": "...", "historyId": "..." } }</pre>
 *
 * <h2>Identifier discipline</h2>
 * <ul>
 *   <li>{@code message.messageId} — Pub/Sub delivery identifier (not stored as Gmail state)</li>
 *   <li>{@code historyId} — Gmail mailbox cursor (stored in {@code gmail_sources.last_history_id})</li>
 *   <li>Gmail message ID — identifies individual emails; NOT present in push notifications</li>
 * </ul>
 *
 * <h2>Acknowledgement behaviour</h2>
 * <p>HTTP 204 is returned when the push has been safely processed or idempotently ignored.
 * HTTP 5xx is returned for transient failures; Pub/Sub will redeliver.
 * HTTP 4xx (except 401) is returned for non-retryable bad requests.
 */
@RestController
@RequestMapping("/api/internal/gmail/pubsub")
public class PubSubPushController {

    private final PubSubJwtValidator jwtValidator;
    private final GmailNotificationHandler notificationHandler;
    private final ObjectMapper objectMapper;

    public PubSubPushController(PubSubJwtValidator jwtValidator,
                                GmailNotificationHandler notificationHandler,
                                ObjectMapper objectMapper) {
        this.jwtValidator = jwtValidator;
        this.notificationHandler = notificationHandler;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/push")
    public ResponseEntity<Void> handlePush(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody PubSubPushEnvelope envelope) {

        // Step 1: Authenticate — validate the Google-signed JWT before reading any payload
        String jwt = extractBearer(authHeader);
        if (!jwtValidator.isValid(jwt)) {
            // Do not log the Authorization header or JWT value
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Step 2: Validate envelope structure
        if (envelope == null || envelope.getMessage() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        PubSubMessage message = envelope.getMessage();

        // Step 3: Decode the Base64 data field
        String rawData = message.getData();
        if (rawData == null || rawData.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        GmailNotification notification;
        try {
            byte[] decoded = Base64.getDecoder().decode(rawData);
            notification = objectMapper.readValue(decoded, GmailNotification.class);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        // Step 4: Validate Gmail notification fields
        String emailAddress = notification.getEmailAddress();
        String historyId = notification.getHistoryId();

        if (emailAddress == null || emailAddress.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        if (historyId == null || historyId.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        // Step 5: Delegate to the handler (cursor tracking only — no email content fetched)
        GmailNotificationHandler.HandlerResult result =
                notificationHandler.handle(emailAddress, historyId);

        return switch (result) {
            case CURSOR_ADVANCED, ALREADY_SEEN ->
                    // Both are safe outcomes: acknowledge so Pub/Sub stops redelivering
                    ResponseEntity.noContent().build();
            case SOURCE_NOT_FOUND ->
                    // Unregistered source — non-retryable; acknowledge to avoid infinite loop
                    ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        };
    }

    private String extractBearer(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Internal DTOs for deserializing the Pub/Sub push envelope
    // -------------------------------------------------------------------------

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PubSubPushEnvelope {
        private PubSubMessage message;
        private String subscription;

        public PubSubMessage getMessage() { return message; }
        public void setMessage(PubSubMessage message) { this.message = message; }
        public String getSubscription() { return subscription; }
        public void setSubscription(String subscription) { this.subscription = subscription; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PubSubMessage {
        private String data;
        private String messageId;
        private String publishTime;
        private Map<String, String> attributes;

        public String getData() { return data; }
        public void setData(String data) { this.data = data; }
        public String getMessageId() { return messageId; }
        public void setMessageId(String messageId) { this.messageId = messageId; }
        public String getPublishTime() { return publishTime; }
        public void setPublishTime(String publishTime) { this.publishTime = publishTime; }
        public Map<String, String> getAttributes() { return attributes; }
        public void setAttributes(Map<String, String> attributes) { this.attributes = attributes; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GmailNotification {
        private String emailAddress;
        private String historyId;

        public String getEmailAddress() { return emailAddress; }
        public void setEmailAddress(String emailAddress) { this.emailAddress = emailAddress; }
        public String getHistoryId() { return historyId; }
        public void setHistoryId(String historyId) { this.historyId = historyId; }
    }
}
