-- V14: Account soft-deletion support
-- Adds deleted_at and anonymized_at timestamps to the users table
-- to support authenticated account deletion with PII anonymisation.
--
-- Deletion semantics:
--   * Authentication credentials (email → anonymised, password_hash → removed)
--   * deleted_at  = timestamp when deletion was requested
--   * anonymized_at = timestamp when student PII fields were anonymised
--
-- Student registration_number and neopat_id are NOT removed because
-- they form the institutional system of record used for historical shortlist matching.
--
-- PlacementDrive, Application, ShortlistEntry, and all placement history records
-- are NOT deleted — they belong to the university system of record.

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS anonymized_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_users_deleted_at ON users (deleted_at)
    WHERE deleted_at IS NOT NULL;
