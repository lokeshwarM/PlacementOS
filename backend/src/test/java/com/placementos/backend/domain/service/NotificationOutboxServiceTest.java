package com.placementos.backend.domain.service;

import com.placementos.backend.domain.entity.Notification;
import com.placementos.backend.domain.entity.NotificationOutbox;
import com.placementos.backend.domain.enums.NotificationChannel;
import com.placementos.backend.domain.enums.NotificationStatus;
import com.placementos.backend.domain.enums.OutboxStatus;
import com.placementos.backend.domain.repository.NotificationOutboxRepository;
import com.placementos.backend.domain.repository.NotificationRepository;
import com.placementos.backend.domain.service.provider.NotificationDeliveryResult;
import com.placementos.backend.domain.service.provider.TelegramNotificationProvider;
import com.placementos.backend.domain.service.provider.WhatsAppNotificationProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class NotificationOutboxServiceTest {

    @Mock
    private NotificationOutboxRepository outboxRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private WhatsAppNotificationProvider whatsAppProvider;

    @Mock
    private TelegramNotificationProvider telegramProvider;

    private NotificationOutboxService outboxService;

    private Notification notification;
    private NotificationOutbox outbox;

    @BeforeEach
    void setUp() {
        outboxService = new NotificationOutboxService(
                outboxRepository,
                notificationRepository,
                whatsAppProvider,
                telegramProvider
        );

        notification = new Notification();
        notification.setId(10L);
        notification.setStatus(NotificationStatus.PENDING);

        outbox = new NotificationOutbox();
        outbox.setId(100L);
        outbox.setNotification(notification);
        outbox.setChannel(NotificationChannel.WHATSAPP);
        outbox.setRecipient("+919876543210");
        outbox.setPayload("Message body");
        outbox.setIdempotencyKey("test-key-1");
        outbox.setStatus(OutboxStatus.PENDING);
        outbox.setAttemptCount(0);
        outbox.setMaxAttempts(3);
        outbox.setAvailableAt(Instant.now());
    }

    @Test
    void processPendingOutbox_successfulDelivery_marksSent() {
        when(outboxRepository.findDueOutboxRecords(any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(outbox));
        when(whatsAppProvider.sendWhatsAppMessage("+919876543210", "Message body", "test-key-1"))
                .thenReturn(NotificationDeliveryResult.success("wa-msg-123"));

        int sent = outboxService.processPendingOutbox(10);

        assertEquals(1, sent);
        assertEquals(OutboxStatus.SENT, outbox.getStatus());
        assertEquals(1, outbox.getAttemptCount());
        assertNotNull(outbox.getProcessedAt());
        assertEquals(NotificationStatus.SENT, notification.getStatus());

        verify(outboxRepository).save(outbox);
        verify(notificationRepository).save(notification);
    }

    @Test
    void processPendingOutbox_retryableFailure_schedulesRetrying() {
        when(outboxRepository.findDueOutboxRecords(any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(outbox));
        when(whatsAppProvider.sendWhatsAppMessage(any(), any(), any()))
                .thenReturn(NotificationDeliveryResult.retryableFailure("Network timeout"));

        int sent = outboxService.processPendingOutbox(10);

        assertEquals(0, sent);
        assertEquals(OutboxStatus.RETRYING, outbox.getStatus());
        assertEquals(1, outbox.getAttemptCount());
        assertEquals("Network timeout", outbox.getLastError());

        verify(outboxRepository).save(outbox);
    }

    @Test
    void processPendingOutbox_maxAttemptsExceeded_marksFailed() {
        outbox.setAttemptCount(2); // this will be the 3rd attempt

        when(outboxRepository.findDueOutboxRecords(any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(outbox));
        when(whatsAppProvider.sendWhatsAppMessage(any(), any(), any()))
                .thenReturn(NotificationDeliveryResult.retryableFailure("Repeated timeout"));

        int sent = outboxService.processPendingOutbox(10);

        assertEquals(0, sent);
        assertEquals(OutboxStatus.FAILED, outbox.getStatus());
        assertEquals(3, outbox.getAttemptCount());
        assertEquals(NotificationStatus.FAILED, notification.getStatus());

        verify(outboxRepository).save(outbox);
        verify(notificationRepository).save(notification);
    }

    @Test
    void processPendingOutbox_telegramDelivery_success() {
        NotificationOutbox tgOutbox = new NotificationOutbox();
        tgOutbox.setId(200L);
        tgOutbox.setNotification(notification);
        tgOutbox.setChannel(NotificationChannel.TELEGRAM);
        tgOutbox.setRecipient("123456789");
        tgOutbox.setPayload("Telegram alert");
        tgOutbox.setIdempotencyKey("tg-key-1");
        tgOutbox.setStatus(OutboxStatus.PENDING);
        tgOutbox.setAttemptCount(0);
        tgOutbox.setMaxAttempts(3);
        tgOutbox.setAvailableAt(Instant.now());

        when(outboxRepository.findDueOutboxRecords(any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(tgOutbox));
        when(telegramProvider.sendTelegramMessage("123456789", "Telegram alert", "tg-key-1"))
                .thenReturn(NotificationDeliveryResult.success("tg-msg-456"));

        int sent = outboxService.processPendingOutbox(10);

        assertEquals(1, sent);
        assertEquals(OutboxStatus.SENT, tgOutbox.getStatus());
        assertEquals(1, tgOutbox.getAttemptCount());
        assertNotNull(tgOutbox.getProcessedAt());
        assertEquals(NotificationStatus.SENT, notification.getStatus());

        verify(outboxRepository).save(tgOutbox);
        verify(notificationRepository).save(notification);
    }

    @Test
    void processPendingOutbox_telegramPermanentFailure_marksFailedImmediately() {
        NotificationOutbox tgOutbox = new NotificationOutbox();
        tgOutbox.setId(201L);
        tgOutbox.setNotification(notification);
        tgOutbox.setChannel(NotificationChannel.TELEGRAM);
        tgOutbox.setRecipient("123456789");
        tgOutbox.setPayload("Telegram alert");
        tgOutbox.setIdempotencyKey("tg-key-2");
        tgOutbox.setStatus(OutboxStatus.PENDING);
        tgOutbox.setAttemptCount(0);
        tgOutbox.setMaxAttempts(3);
        tgOutbox.setAvailableAt(Instant.now());

        when(outboxRepository.findDueOutboxRecords(any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(tgOutbox));
        when(telegramProvider.sendTelegramMessage("123456789", "Telegram alert", "tg-key-2"))
                .thenReturn(NotificationDeliveryResult.permanentFailure("Telegram user blocked bot"));

        int sent = outboxService.processPendingOutbox(10);

        assertEquals(0, sent);
        assertEquals(OutboxStatus.FAILED, tgOutbox.getStatus());
        assertEquals(1, tgOutbox.getAttemptCount()); // stopped immediately, not retried!
        assertEquals("Telegram user blocked bot", tgOutbox.getLastError());
        assertEquals(NotificationStatus.FAILED, notification.getStatus());

        verify(outboxRepository).save(tgOutbox);
        verify(notificationRepository).save(notification);
    }
}

