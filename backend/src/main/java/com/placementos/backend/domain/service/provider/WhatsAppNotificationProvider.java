package com.placementos.backend.domain.service.provider;

/**
 * Abstraction for WhatsApp messaging providers (e.g. WhatsApp Cloud API, Infobip, Twilio, Mock).
 */
public interface WhatsAppNotificationProvider {

    /**
     * Sends a WhatsApp message to the specified recipient phone number.
     *
     * @param recipientPhone Destination phone number (E.164 format or standard Indian mobile format)
     * @param messageText    Rendered plaintext message content
     * @param idempotencyKey Deterministic idempotency key for safe retries
     * @return Delivery result with status and error details
     */
    NotificationDeliveryResult sendWhatsAppMessage(String recipientPhone, String messageText, String idempotencyKey);
}
