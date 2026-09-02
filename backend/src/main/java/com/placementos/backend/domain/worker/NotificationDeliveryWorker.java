package com.placementos.backend.domain.worker;

import com.placementos.backend.domain.service.NotificationOutboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Background worker that polls and drains the transactional notification outbox.
 */
@Component
public class NotificationDeliveryWorker {

    private static final Logger log = LoggerFactory.getLogger(NotificationDeliveryWorker.class);

    private final NotificationOutboxService outboxService;

    public NotificationDeliveryWorker(NotificationOutboxService outboxService) {
        this.outboxService = outboxService;
    }

    /**
     * Periodically process due outbox records every 2 seconds.
     */
    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:2000}")
    public void processOutbox() {
        try {
            int dispatched = outboxService.processPendingOutbox(50);
            if (dispatched > 0) {
                log.info("NotificationDeliveryWorker processed {} outbox records", dispatched);
            }
        } catch (Exception e) {
            log.error("Error in NotificationDeliveryWorker execution: {}", e.getMessage(), e);
        }
    }
}
