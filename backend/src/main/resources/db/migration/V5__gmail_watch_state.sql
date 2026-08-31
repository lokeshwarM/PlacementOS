-- V5: Add Gmail watch state tracking columns to gmail_sources
--
-- last_history_id: Opaque string cursor identifying the last mailbox position
--   processed by this application. This is a synchronization cursor, NOT a count of
--   messages processed. Type VARCHAR(255) matches the Gmail API contract: the Java
--   client library returns historyId as String from WatchResponse.getHistoryId().
--
-- watch_expiration: Timestamp when the current Gmail watch registration expires.
--   Gmail watch registrations expire within approximately 7 days (604800 seconds).
--   A future scheduled maintenance workflow must renew the watch before expiration.
--
-- watch_status: Tracks the current watch lifecycle state.
--   NONE     = no watch registered
--   ACTIVE   = watch registered, not yet expired
--   EXPIRED  = watch registration has expired, renewal required
ALTER TABLE gmail_sources
    ADD COLUMN last_history_id  VARCHAR(255),
    ADD COLUMN watch_expiration TIMESTAMPTZ,
    ADD COLUMN watch_status     VARCHAR(50) NOT NULL DEFAULT 'NONE';
