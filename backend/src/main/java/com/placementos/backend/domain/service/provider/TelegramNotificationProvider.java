package com.placementos.backend.domain.service.provider;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.placementos.backend.config.TelegramProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Production-ready provider for delivering notifications via the Telegram Bot API over HTTPS.
 *
 * Enforces:
 * - Bot token is injected via configuration and NEVER logged.
 * - Proper classification of transient (retryable) vs permanent delivery failures.
 * - Safe fallback/mock mode when bot credentials are not configured (local dev/test).
 */
@Component
public class TelegramNotificationProvider {

    private static final Logger log = LoggerFactory.getLogger(TelegramNotificationProvider.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final TelegramProperties telegramProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @org.springframework.beans.factory.annotation.Autowired
    public TelegramNotificationProvider(TelegramProperties telegramProperties, ObjectMapper objectMapper) {
        this(telegramProperties, objectMapper, HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .build());
    }

    public TelegramNotificationProvider(TelegramProperties telegramProperties,
                                        ObjectMapper objectMapper,
                                        HttpClient httpClient) {
        this.telegramProperties = telegramProperties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    /**
     * Sends a text message to a Telegram chat via the Telegram Bot API sendMessage endpoint.
     *
     * @param recipientChatId Telegram chat ID (numeric string)
     * @param messageText     Rendered text content (supports safe markdown/plain text)
     * @param idempotencyKey  Deterministic idempotency key for tracing
     * @return Structured delivery result indicating success, retryable failure, or permanent failure
     */
    public NotificationDeliveryResult sendTelegramMessage(String recipientChatId,
                                                          String messageText,
                                                          String idempotencyKey) {
        return sendTelegramMessage(recipientChatId, messageText, idempotencyKey, null);
    }

    /**
     * Sends a text message to a Telegram chat with an optional inline keyboard markup.
     *
     * @param recipientChatId Telegram chat ID (numeric string)
     * @param messageText     Rendered text content
     * @param idempotencyKey  Deterministic idempotency key for tracing
     * @param replyMarkup     Optional inline keyboard markup Map (serialized to JSON)
     * @return Structured delivery result
     */
    public NotificationDeliveryResult sendTelegramMessage(String recipientChatId,
                                                          String messageText,
                                                          String idempotencyKey,
                                                          Map<String, Object> replyMarkup) {
        if (recipientChatId == null || recipientChatId.isBlank() || recipientChatId.equalsIgnoreCase("UNLINKED")) {
            return NotificationDeliveryResult.permanentFailure("Cannot send Telegram message: invalid or unlinked chat ID");
        }

        // Mock / Development mode when token is unconfigured or set to mock
        if (!telegramProperties.isConfigured()) {
            String mockMsgId = "mock-tg-" + UUID.randomUUID().toString().substring(0, 8);
            log.info("TelegramNotificationProvider (mock mode): message dispatched to chatId={}, key={}, msgId={}",
                    recipientChatId, idempotencyKey, mockMsgId);
            return NotificationDeliveryResult.success(mockMsgId);
        }

        try {
            String baseUrl = telegramProperties.getApiBaseUrl();
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }

            // Construct endpoint: https://api.telegram.org/bot<TOKEN>/sendMessage
            URI uri = URI.create(baseUrl + "/bot" + telegramProperties.getBotToken() + "/sendMessage");

            Map<String, Object> payload = new HashMap<>();
            payload.put("chat_id", recipientChatId);
            payload.put("text", messageText);
            payload.put("parse_mode", "Markdown");

            if (replyMarkup != null && !replyMarkup.isEmpty()) {
                payload.put("reply_markup", replyMarkup);
            }

            String requestBody = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();
            String responseBody = response.body();

            return parseTelegramResponse(statusCode, responseBody, recipientChatId, idempotencyKey);

        } catch (java.net.http.HttpTimeoutException e) {
            log.warn("Telegram API request timed out for chatId={}, key={}", recipientChatId, idempotencyKey);
            return NotificationDeliveryResult.retryableFailure("Telegram API request timed out: " + e.getMessage());
        } catch (java.io.IOException e) {
            log.warn("Telegram API network I/O error for chatId={}, key={}: {}", recipientChatId, idempotencyKey, e.getMessage());
            return NotificationDeliveryResult.retryableFailure("Network error communicating with Telegram API: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Telegram API request interrupted for chatId={}, key={}", recipientChatId, idempotencyKey);
            return NotificationDeliveryResult.retryableFailure("Telegram delivery interrupted");
        } catch (Exception e) {
            log.error("Unexpected error sending Telegram message for chatId={}, key={}: {}",
                    recipientChatId, idempotencyKey, e.getMessage(), e);
            return NotificationDeliveryResult.retryableFailure("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Answers a callback query from an inline keyboard button.
     */
    public boolean answerCallbackQuery(String callbackQueryId, String text) {
        if (!telegramProperties.isConfigured()) {
            return true;
        }

        try {
            String baseUrl = telegramProperties.getApiBaseUrl();
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }

            URI uri = URI.create(baseUrl + "/bot" + telegramProperties.getBotToken() + "/answerCallbackQuery");

            Map<String, Object> payload = new HashMap<>();
            payload.put("callback_query_id", callbackQueryId);
            if (text != null && !text.isBlank()) {
                payload.put("text", text);
            }

            String requestBody = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            log.warn("Failed to answer Telegram callback query id={}: {}", callbackQueryId, e.getMessage());
            return false;
        }
    }

    private NotificationDeliveryResult parseTelegramResponse(int statusCode,
                                                             String responseBody,
                                                             String recipientChatId,
                                                             String idempotencyKey) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            boolean ok = root.path("ok").asBoolean(false);

            if (ok && statusCode == 200) {
                String messageId = root.path("result").path("message_id").asText();
                log.info("Telegram message successfully sent: chatId={}, key={}, messageId={}",
                        recipientChatId, idempotencyKey, messageId);
                return NotificationDeliveryResult.success(messageId);
            }

            int errorCode = root.path("error_code").asInt(statusCode);
            String description = root.path("description").asText("Unknown Telegram API error");

            // Categorize errors safely
            if (statusCode == 429) {
                // Rate limited
                int retryAfter = root.path("parameters").path("retry_after").asInt(5);
                log.warn("Telegram rate limited for chatId={}, key={}: retry_after={}s", recipientChatId, idempotencyKey, retryAfter);
                return NotificationDeliveryResult.retryableFailure("Telegram rate limited (retry after " + retryAfter + "s): " + description);
            }

            if (statusCode >= 500) {
                // Telegram server error
                log.warn("Telegram server error (HTTP {}) for chatId={}, key={}: {}", statusCode, recipientChatId, idempotencyKey, description);
                return NotificationDeliveryResult.retryableFailure("Telegram server error (HTTP " + statusCode + "): " + description);
            }

            if (statusCode == 403 || description.toLowerCase().contains("blocked by the user") || description.toLowerCase().contains("user is deactivated")) {
                // User blocked bot or deactivated account -> Permanent failure
                log.warn("Telegram delivery permanently failed (user blocked / deactivated) for chatId={}, key={}: {}",
                        recipientChatId, idempotencyKey, description);
                return NotificationDeliveryResult.permanentFailure("Telegram user blocked bot or deactivated: " + description);
            }

            if (statusCode == 400 && (description.toLowerCase().contains("chat not found") || description.toLowerCase().contains("chat_id is empty"))) {
                // Invalid chat ID -> Permanent failure
                log.warn("Telegram chat not found for chatId={}, key={}: {}", recipientChatId, idempotencyKey, description);
                return NotificationDeliveryResult.permanentFailure("Telegram chat not found: " + description);
            }

            if (statusCode == 401) {
                // Invalid bot token -> Permanent failure
                log.error("Telegram bot token is unauthorized / invalid (HTTP 401)");
                return NotificationDeliveryResult.permanentFailure("Telegram bot token is invalid or unauthorized");
            }

            // Other 4xx errors
            log.warn("Telegram delivery failed (HTTP {}) for chatId={}, key={}: {}", statusCode, recipientChatId, idempotencyKey, description);
            return NotificationDeliveryResult.permanentFailure("Telegram API error (HTTP " + statusCode + "): " + description);

        } catch (Exception e) {
            log.error("Failed to parse Telegram API response (HTTP {}): {}", statusCode, e.getMessage());
            if (statusCode >= 500) {
                return NotificationDeliveryResult.retryableFailure("Telegram HTTP " + statusCode + " with unparseable response");
            }
            return NotificationDeliveryResult.permanentFailure("Telegram HTTP " + statusCode + " with unparseable response");
        }
    }
}
