package com.placementos.backend.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * JPA entity for tracking received Telegram update IDs to ensure idempotent webhook processing.
 */
@Entity
@Table(name = "telegram_webhook_updates")
public class TelegramWebhookUpdate {

    @Id
    @Column(name = "update_id", nullable = false)
    private Long updateId;

    @Column(name = "processed_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMPTZ DEFAULT NOW()")
    private Instant processedAt;

    public TelegramWebhookUpdate() {}

    public TelegramWebhookUpdate(Long updateId) {
        this.updateId = updateId;
        this.processedAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (processedAt == null) processedAt = Instant.now();
    }

    public Long getUpdateId() { return updateId; }
    public void setUpdateId(Long updateId) { this.updateId = updateId; }

    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TelegramWebhookUpdate other)) return false;
        return updateId != null && updateId.equals(other.updateId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
