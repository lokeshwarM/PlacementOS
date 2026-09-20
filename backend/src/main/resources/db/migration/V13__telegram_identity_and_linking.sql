-- =============================================================================
-- V13: Telegram Identity, One-Time Linking Tokens, and Webhook Deduplication
-- =============================================================================

-- 1. Create telegram_identities table
CREATE TABLE telegram_identities (
    id                  BIGSERIAL PRIMARY KEY,
    student_id          BIGINT NOT NULL UNIQUE REFERENCES students(id) ON DELETE CASCADE,
    telegram_chat_id    BIGINT NOT NULL UNIQUE,
    telegram_user_id    BIGINT,
    telegram_username   VARCHAR(255),
    linked_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_telegram_identities_student_id ON telegram_identities(student_id);
CREATE INDEX idx_telegram_identities_chat_id ON telegram_identities(telegram_chat_id);
CREATE INDEX idx_telegram_identities_user_id ON telegram_identities(telegram_user_id);

-- 2. Create telegram_link_tokens table for secure one-time account linking
CREATE TABLE telegram_link_tokens (
    id                  BIGSERIAL PRIMARY KEY,
    token_hash          VARCHAR(64) NOT NULL UNIQUE,
    student_id          BIGINT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    expires_at          TIMESTAMPTZ NOT NULL,
    used_at             TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_telegram_link_tokens_student_id ON telegram_link_tokens(student_id);
CREATE INDEX idx_telegram_link_tokens_expires_at ON telegram_link_tokens(expires_at);

-- 3. Create telegram_webhook_updates table for idempotent webhook handling
CREATE TABLE telegram_webhook_updates (
    update_id           BIGINT PRIMARY KEY,
    processed_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
