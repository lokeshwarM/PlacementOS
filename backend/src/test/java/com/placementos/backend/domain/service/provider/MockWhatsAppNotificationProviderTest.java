package com.placementos.backend.domain.service.provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MockWhatsAppNotificationProviderTest {

    private MockWhatsAppNotificationProvider provider;

    @BeforeEach
    void setUp() {
        provider = new MockWhatsAppNotificationProvider();
        provider.clear();
    }

    @Test
    void sendWhatsAppMessage_success() {
        NotificationDeliveryResult result = provider.sendWhatsAppMessage(
                "+919876543210",
                "Hello Alice, you are eligible!",
                "eligibility:student:1:drive:10"
        );

        assertTrue(result.success());
        assertNotNull(result.externalMessageId());
        assertEquals(1, provider.getDeliveredCount());

        MockWhatsAppNotificationProvider.DeliveryRecord record = provider.getDeliveredMessages().get(0);
        assertEquals("+919876543210", record.recipient());
        assertEquals("eligibility:student:1:drive:10", record.idempotencyKey());
    }

    @Test
    void sendWhatsAppMessage_simulateRetryableFailure() {
        provider.setSimulateRetryableFailure(true);

        NotificationDeliveryResult result = provider.sendWhatsAppMessage(
                "+919876543210",
                "Hello",
                "key-1"
        );

        assertFalse(result.success());
        assertTrue(result.retryable());
        assertEquals(0, provider.getDeliveredCount());
    }
}
