package com.placementos.backend.domain.service.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory Mock implementation of {@link WhatsAppNotificationProvider}.
 * Never makes external network requests.
 * Records delivery logs for automated test assertions.
 */
@Component
public class MockWhatsAppNotificationProvider implements WhatsAppNotificationProvider {

    private static final Logger log = LoggerFactory.getLogger(MockWhatsAppNotificationProvider.class);

    public record DeliveryRecord(
            String recipient,
            String messageText,
            String idempotencyKey,
            String messageId,
            Instant deliveredAt
    ) {}

    private final List<DeliveryRecord> deliveredMessages = new CopyOnWriteArrayList<>();
    private volatile boolean simulateFailure = false;
    private volatile boolean simulateRetryableFailure = false;

    @Override
    public NotificationDeliveryResult sendWhatsAppMessage(String recipientPhone, String messageText, String idempotencyKey) {
        if (simulateFailure) {
            log.warn("MockWhatsAppNotificationProvider simulating permanent failure for key={}", idempotencyKey);
            return NotificationDeliveryResult.permanentFailure("Simulated permanent WhatsApp provider error");
        }

        if (simulateRetryableFailure) {
            log.warn("MockWhatsAppNotificationProvider simulating retryable failure for key={}", idempotencyKey);
            return NotificationDeliveryResult.retryableFailure("Simulated rate-limit / network timeout");
        }

        String messageId = "mock-wa-" + UUID.randomUUID().toString().substring(0, 8);
        DeliveryRecord record = new DeliveryRecord(
                recipientPhone,
                messageText,
                idempotencyKey,
                messageId,
                Instant.now()
        );
        deliveredMessages.add(record);

        log.info("Mock WhatsApp message delivered: id={}, recipient={}, key={}", messageId, recipientPhone, idempotencyKey);
        return NotificationDeliveryResult.success(messageId);
    }

    public List<DeliveryRecord> getDeliveredMessages() {
        return Collections.unmodifiableList(new ArrayList<>(deliveredMessages));
    }

    public int getDeliveredCount() {
        return deliveredMessages.size();
    }

    public void clear() {
        deliveredMessages.clear();
        simulateFailure = false;
        simulateRetryableFailure = false;
    }

    public void setSimulateFailure(boolean simulateFailure) {
        this.simulateFailure = simulateFailure;
    }

    public void setSimulateRetryableFailure(boolean simulateRetryableFailure) {
        this.simulateRetryableFailure = simulateRetryableFailure;
    }
}
