package com.placementos.backend.domain.service.provider;

/**
 * Result of a notification delivery attempt via a notification provider.
 */
public record NotificationDeliveryResult(
        boolean success,
        String externalMessageId,
        String errorMessage,
        boolean retryable
) {
    public static NotificationDeliveryResult success(String externalMessageId) {
        return new NotificationDeliveryResult(true, externalMessageId, null, false);
    }

    public static NotificationDeliveryResult retryableFailure(String errorMessage) {
        return new NotificationDeliveryResult(false, null, errorMessage, true);
    }

    public static NotificationDeliveryResult permanentFailure(String errorMessage) {
        return new NotificationDeliveryResult(false, null, errorMessage, false);
    }
}
