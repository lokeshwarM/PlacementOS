-- =============================================================================
-- V8: Placement Roles and Schema Evolution
--
-- 1. Creates placement_roles table for multi-role support per placement drive.
-- 2. Enforces unique constraint on placement_drives(source_email_id) for idempotent ingestion.
-- 3. Adds NON_PLACEMENT and EXTRACTED to processed_emails.processing_status check constraint.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- placement_roles
-- Represents an individual job role / position within a placement drive.
-- Role-specific eligibility is stored in eligibility_criteria (JSONB).
-- Common eligibility remains on the parent placement_drives table.
-- uq_placement_roles_drive_title guarantees role uniqueness within a drive.
-- -----------------------------------------------------------------------------
CREATE TABLE placement_roles (
    id                   BIGSERIAL       PRIMARY KEY,
    placement_drive_id   BIGINT          NOT NULL REFERENCES placement_drives (id) ON DELETE CASCADE,
    role_title           VARCHAR(255)    NOT NULL,
    role_description     TEXT,
    role_order           INTEGER         NOT NULL DEFAULT 1,
    eligibility_criteria JSONB,
    created_at           TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_placement_roles_drive_title UNIQUE (placement_drive_id, role_title)
);

CREATE INDEX idx_placement_roles_drive_id ON placement_roles (placement_drive_id);

-- -----------------------------------------------------------------------------
-- placement_drives idempotency constraint
-- Ensures a single source Gmail message can map to at most one PlacementDrive.
-- -----------------------------------------------------------------------------
ALTER TABLE placement_drives 
    ADD CONSTRAINT uq_placement_drives_source_email UNIQUE (source_email_id);

-- -----------------------------------------------------------------------------
-- processed_emails check constraint update
-- Adds NON_PLACEMENT and EXTRACTED states to the processing lifecycle.
-- -----------------------------------------------------------------------------
ALTER TABLE processed_emails DROP CONSTRAINT processed_emails_processing_status_check;

ALTER TABLE processed_emails ADD CONSTRAINT processed_emails_processing_status_check 
    CHECK (processing_status IN ('PENDING', 'PROCESSED', 'FAILED', 'DUPLICATE', 'DISCOVERED', 'QUEUED', 'RETRIEVED', 'NON_PLACEMENT', 'EXTRACTED'));
