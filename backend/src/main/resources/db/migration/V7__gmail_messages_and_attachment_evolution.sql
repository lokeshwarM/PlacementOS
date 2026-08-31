-- =============================================================================
-- V7: Gmail Messages Persistence and Attachment Evolution
--
-- 1. Creates gmail_messages table to store acquired, normalized Gmail message content.
-- 2. Evolves attachments table to allow linking raw attachments to gmail_messages
--    (via gmail_message_record_id) before placement drive classification.
-- 3. Adds RETRIEVED to processed_emails.processing_status check constraint.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- gmail_messages
-- -----------------------------------------------------------------------------
CREATE TABLE gmail_messages (
    id                   BIGSERIAL       PRIMARY KEY,
    message_id           VARCHAR(255)    NOT NULL,
    gmail_source_id      BIGINT          NOT NULL REFERENCES gmail_sources (id),
    thread_id            VARCHAR(255),
    subject              VARCHAR(500),
    sender               VARCHAR(255),
    recipients           TEXT,
    plain_text_body      TEXT,
    html_body            TEXT,
    snippet              TEXT,
    gmail_internal_date  TIMESTAMPTZ,
    retrieval_status     VARCHAR(50)     NOT NULL DEFAULT 'RETRIEVED'
                             CHECK (retrieval_status IN ('RETRIEVED', 'FAILED')),
    retrieved_at         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_at           TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_gmail_messages_source_message UNIQUE (gmail_source_id, message_id)
);

CREATE INDEX idx_gmail_messages_message_id ON gmail_messages (message_id);
CREATE INDEX idx_gmail_messages_source_id  ON gmail_messages (gmail_source_id);
CREATE INDEX idx_gmail_messages_thread_id  ON gmail_messages (thread_id);

-- -----------------------------------------------------------------------------
-- attachments evolution
-- -----------------------------------------------------------------------------
ALTER TABLE attachments ALTER COLUMN placement_drive_id DROP NOT NULL;
ALTER TABLE attachments ADD COLUMN gmail_message_record_id BIGINT REFERENCES gmail_messages (id) ON DELETE CASCADE;
ALTER TABLE attachments ADD COLUMN attachment_id VARCHAR(255);
ALTER TABLE attachments ADD COLUMN byte_size BIGINT;

CREATE INDEX idx_attachments_gmail_message_record_id ON attachments (gmail_message_record_id);

ALTER TABLE attachments ADD CONSTRAINT chk_attachments_parent 
    CHECK (placement_drive_id IS NOT NULL OR gmail_message_record_id IS NOT NULL);

-- -----------------------------------------------------------------------------
-- processed_emails check constraint update
-- -----------------------------------------------------------------------------
ALTER TABLE processed_emails DROP CONSTRAINT processed_emails_processing_status_check;

ALTER TABLE processed_emails ADD CONSTRAINT processed_emails_processing_status_check 
    CHECK (processing_status IN ('PENDING', 'PROCESSED', 'FAILED', 'DUPLICATE', 'DISCOVERED', 'QUEUED', 'RETRIEVED'));
