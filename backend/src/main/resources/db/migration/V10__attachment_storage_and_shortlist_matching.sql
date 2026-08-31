-- =============================================================================
-- V10: Attachment Storage and Shortlist Candidate Matching Evolution
--
-- 1. Adds sha256_checksum to attachments table.
-- 2. Expands attachments.parsed_status check constraint to support the full
--    document processing lifecycle (DOWNLOADED, PROCESSING, EXTRACTED, OCR_REQUIRED, REVIEW_REQUIRED).
-- 3. Evolves shortlist_entries to link to students and placement_roles, and
--    records deterministic match status, match reason, and raw evidence.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. attachments evolution
-- -----------------------------------------------------------------------------
ALTER TABLE attachments ADD COLUMN sha256_checksum VARCHAR(64);
CREATE INDEX idx_attachments_sha256_checksum ON attachments (sha256_checksum);

ALTER TABLE attachments DROP CONSTRAINT IF EXISTS attachments_parsed_status_check;
ALTER TABLE attachments ADD CONSTRAINT attachments_parsed_status_check
    CHECK (parsed_status IN (
        'PENDING', 'DOWNLOADED', 'PROCESSING', 'EXTRACTED', 'PARSED',
        'FAILED', 'OCR_REQUIRED', 'REVIEW_REQUIRED', 'SKIPPED'
    ));

-- -----------------------------------------------------------------------------
-- 2. shortlist_entries evolution
-- -----------------------------------------------------------------------------
ALTER TABLE shortlist_entries ADD COLUMN student_id BIGINT REFERENCES students (id) ON DELETE SET NULL;
ALTER TABLE shortlist_entries ADD COLUMN placement_role_id BIGINT REFERENCES placement_roles (id) ON DELETE SET NULL;
ALTER TABLE shortlist_entries ADD COLUMN match_status VARCHAR(50) NOT NULL DEFAULT 'UNMATCHED'
    CHECK (match_status IN ('UNMATCHED', 'MATCHED', 'AMBIGUOUS', 'REVIEW_REQUIRED'));
ALTER TABLE shortlist_entries ADD COLUMN match_reason TEXT;
ALTER TABLE shortlist_entries ADD COLUMN raw_evidence TEXT;
ALTER TABLE shortlist_entries ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

CREATE INDEX idx_shortlist_entries_student_id ON shortlist_entries (student_id);
CREATE INDEX idx_shortlist_entries_placement_role_id ON shortlist_entries (placement_role_id);
CREATE INDEX idx_shortlist_entries_match_status ON shortlist_entries (match_status);
CREATE INDEX idx_shortlist_entries_source_attachment_id ON shortlist_entries (source_attachment_id);
