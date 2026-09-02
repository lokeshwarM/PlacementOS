package com.placementos.backend.domain.service;

import com.placementos.backend.domain.entity.Notification;
import com.placementos.backend.domain.entity.NotificationOutbox;
import com.placementos.backend.domain.enums.NotificationChannel;
import com.placementos.backend.domain.enums.NotificationStatus;
import com.placementos.backend.domain.enums.OutboxStatus;
import com.placementos.backend.domain.repository.NotificationOutboxRepository;
import com.placementos.backend.domain.repository.NotificationRepository;
import com.placementos.backend.domain.service.provider.NotificationDeliveryResult;
import com.placementos.backend.domain.service.provider.WhatsAppNotificationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Service responsible for processing transactional notification outbox records,
 * dispatching messages to providers, handling retries with exponential backoff,
 * and maintaining delivery state.
 */
@Service
public class NotificationOutboxService {

    private static final Logger log = LoggerFactory.getLogger(NotificationOutboxService.class);

    private final NotificationOutboxRepository outboxRepository;
    private final NotificationRepository notificationRepository;
    private final WhatsAppNotificationProvider whatsAppProvider;

    public NotificationOutboxService(NotificationOutboxRepository outboxRepository,
                                   NotificationRepository notificationRepository,
                                   WhatsAppNotificationProvider whatsAppProvider) {
        this.outboxRepository = outboxRepository;
        this.notificationRepository = notificationRepository;
        this.whatsAppProvider = whatsAppProvider;
    }

    /**
     * Processes a batch of due outbox records.
     *
     * @param batchSize maximum number of records to process
     * @return count of successfully delivered records in this batch
     */
    @Transactional
    public int processPendingOutbox(int batchSize) {
        Instant now = Instant.now();
        List<NotificationOutbox> dueRecords = outboxRepository.findDueOutboxRecords(
                now,
                PageRequest.of(0, Math.max(1, batchSize))
        );

        if (dueRecords.isEmpty()) {
            return 0;
        }

        int successCount = 0;

        for (NotificationOutbox outbox : dueRecords) {
            try {
                boolean sent = processSingleRecord(outbox);
                if (sent) {
                    successCount++;
                }
            } catch (Exception e) {
                log.error("Unexpected error delivering outbox id={}: {}", outbox.getId(), e.getMessage(), e);
                handleFailure(outbox, e.getMessage(), true);
            }
        }

        return successCount;
    }

    private boolean processSingleRecord(NotificationOutbox outbox) {
        outbox.setStatus(OutboxStatus.PROCESSING);
        outbox.setAttemptCount(outbox.getAttemptCount() + 1);

        NotificationDeliveryResult result;

        if (outbox.getChannel() == NotificationChannel.WHATSAPP) {
            result = whatsAppProvider.sendWhatsAppMessage(
                    outbox.getRecipient(),
                    outbox.getPayload(),
                    outbox.getIdempotencyKey()
            );
        } else {
            // Other channels (e.g. Email / Telegram) can be added cleanly in future milestones
            log.warn("Unsupported delivery channel {} for outbox id={}", outbox.getChannel(), outbox.getId());
            result = NotificationDeliveryResult.permanentFailure("Unsupported channel: " + outbox.getChannel());
        }

        if (result.success()) {
            Instant sentTime = Instant.now();
            outbox.setStatus(OutboxStatus.SENT);
            outbox.setProcessedAt(sentTime);
            outbox.setLastError(null);
            outboxRepository.save(outbox);

            Notification notification = outbox.getNotification();
            if (notification != null) {
                notification.setStatus(NotificationStatus.SENT);
                notification.setSentAt(sentTime);
                notificationRepository.save(notification);
            }

            log.info("Outbox id={} successfully dispatched to recipient {} via {}",
                    outbox.getId(), outbox.getRecipient(), outbox.getChannel());
            return true;
        } else {
            handleFailure(outbox, result.errorMessage(), result.retryable());
            return false;
        }
    }

    private void handleFailure(NotificationOutbox outbox, String errorMessage, boolean isRetryable) {
        outbox.setLastError(errorMessage);

        if (isRetryable && outbox.getAttemptCount() < outbox.getMaxAttempts()) {
            // Exponential backoff: 5s, 10s, 20s...
            long backoffSeconds = 5L * (1L << (outbox.getAttemptCount() - 1));
            outbox.setStatus(OutboxStatus.RETRYING);
            outbox.setAvailableAt(Instant.now().plus(Duration.ofSeconds(backoffSeconds)));
            log.warn("Outbox id={} delivery failed (attempt {}/{}). Retrying in {}s. Reason: {}",
                    outbox.getId(), outbox.getAttemptCount(), outbox.getMaxAttempts(), backoffSeconds, errorMessage);
        } else {
            outbox.setStatus(OutboxStatus.FAILED);
            outbox.setProcessedAt(Instant.now());
            Notification notification = outbox.getNotification();
            if (notification != null) {
                notification.setStatus(NotificationStatus.FAILED);
                notificationRepository.save(notification);
            }
            log.error("Outbox id={} delivery permanently failed after {} attempts. Reason: {}",
                    outbox.getId(), outbox.getAttemptCount(), errorMessage);
        }

        outboxRepository.save(outbox);
    }
}
