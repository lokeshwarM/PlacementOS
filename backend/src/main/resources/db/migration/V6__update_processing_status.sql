-- =============================================================================
-- V6: Update EmailProcessingStatus Constraint
-- 
-- Adds DISCOVERED and QUEUED to the processing_status check constraint to
-- support the granular ingestion lifecycle (History API discovery -> Redis queue).
-- =============================================================================

ALTER TABLE processed_emails DROP CONSTRAINT processed_emails_processing_status_check;

ALTER TABLE processed_emails ADD CONSTRAINT processed_emails_processing_status_check 
    CHECK (processing_status IN ('PENDING', 'PROCESSED', 'FAILED', 'DUPLICATE', 'DISCOVERED', 'QUEUED'));
