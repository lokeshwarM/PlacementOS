-- =============================================================================
-- V11: Application Role Association, Notification Idempotency Key,
--      Transactional Notification Outbox, and Reminder Task Lifecycle
-- =============================================================================

-- 1. Evolve applications with role awareness
ALTER TABLE applications
    ADD COLUMN placement_role_id BIGINT REFERENCES placement_roles(id) ON DELETE SET NULL;

CREATE INDEX idx_applications_placement_role_id ON applications(placement_role_id);

-- 2. Evolve notifications for durable idempotency and role awareness
-- Drop the overly restrictive composite unique constraint that blocked recurring reminders
ALTER TABLE notifications
    DROP CONSTRAINT IF EXISTS uq_notifications_idempotency;

ALTER TABLE notifications
    ADD COLUMN idempotency_key VARCHAR(255),
    ADD COLUMN placement_role_id BIGINT REFERENCES placement_roles(id) ON DELETE SET NULL,
    ADD COLUMN message_payload TEXT;

-- Backfill idempotency_key for existing legacy notification rows if any exist
UPDATE notifications
SET idempotency_key = 'legacy:notif:' || id
WHERE idempotency_key IS NULL;

ALTER TABLE notifications
    ALTER COLUMN idempotency_key SET NOT NULL;

ALTER TABLE notifications
    ADD CONSTRAINT uq_notifications_idempotency_key UNIQUE (idempotency_key);

CREATE INDEX idx_notifications_idempotency_key ON notifications(idempotency_key);
CREATE INDEX idx_notifications_student_drive ON notifications(student_id, placement_drive_id);
CREATE INDEX idx_notifications_placement_role_id ON notifications(placement_role_id);

-- 3. Create transactional notification outbox table
CREATE TABLE notification_outbox (
    id                  BIGSERIAL PRIMARY KEY,
    notification_id     BIGINT NOT NULL REFERENCES notifications(id) ON DELETE CASCADE,
    idempotency_key     VARCHAR(255) NOT NULL UNIQUE,
    status              VARCHAR(50) NOT NULL DEFAULT 'PENDING'
                            CHECK (status IN ('PENDING', 'PROCESSING', 'SENT', 'FAILED', 'RETRYING', 'CANCELLED')),
    channel             VARCHAR(50) NOT NULL
                            CHECK (channel IN ('WHATSAPP', 'TELEGRAM', 'EMAIL', 'IN_APP')),
    recipient           VARCHAR(255) NOT NULL,
    payload             TEXT NOT NULL,
    attempt_count       INTEGER NOT NULL DEFAULT 0,
    max_attempts        INTEGER NOT NULL DEFAULT 3,
    available_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    processed_at        TIMESTAMPTZ,
    last_error          TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notification_outbox_status_available ON notification_outbox(status, available_at);
CREATE INDEX idx_notification_outbox_notification_id ON notification_outbox(notification_id);
CREATE INDEX idx_notification_outbox_idempotency_key ON notification_outbox(idempotency_key);

-- 4. Evolve reminder_tasks for recurring interval scheduling and cancel tracking
ALTER TABLE reminder_tasks
    ADD COLUMN placement_role_id BIGINT REFERENCES placement_roles(id) ON DELETE SET NULL,
    ADD COLUMN interval_minutes INTEGER NOT NULL DEFAULT 60,
    ADD COLUMN max_reminders INTEGER NOT NULL DEFAULT 5,
    ADD COLUMN reminders_sent INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN cancel_reason VARCHAR(255),
    ADD COLUMN last_reminder_at TIMESTAMPTZ;

CREATE INDEX idx_reminder_tasks_status_scheduled ON reminder_tasks(status, scheduled_for);
CREATE INDEX idx_reminder_tasks_placement_role_id ON reminder_tasks(placement_role_id);
