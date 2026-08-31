-- =============================================================================
-- V9: Student Fields and Student Eligibility Results
--
-- 1. Adds degree, specialization, standing_arrears, and gender columns to students table.
--    standing_arrears is nullable with NO default (NULL = unknown, 0 = 0 arrears, >0 = arrears).
-- 2. Creates student_eligibility_results table for persisted, explainable per-role
--    eligibility evaluation decisions with unique constraint (student_id, placement_role_id).
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. Alter students table
-- -----------------------------------------------------------------------------
ALTER TABLE students
    ADD COLUMN degree VARCHAR(50),
    ADD COLUMN specialization VARCHAR(100),
    ADD COLUMN standing_arrears INTEGER,
    ADD COLUMN gender VARCHAR(20);

-- -----------------------------------------------------------------------------
-- 2. student_eligibility_results
-- -----------------------------------------------------------------------------
CREATE TABLE student_eligibility_results (
    id                   BIGSERIAL       PRIMARY KEY,
    student_id           BIGINT          NOT NULL REFERENCES students (id) ON DELETE CASCADE,
    placement_drive_id   BIGINT          NOT NULL REFERENCES placement_drives (id) ON DELETE CASCADE,
    placement_role_id    BIGINT          NOT NULL REFERENCES placement_roles (id) ON DELETE CASCADE,
    decision             VARCHAR(50)     NOT NULL,
    criteria_results     JSONB           NOT NULL,
    evaluator_version    VARCHAR(50)     NOT NULL,
    evaluated_at         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_at           TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_eligibility_decision CHECK (decision IN ('ELIGIBLE', 'NOT_ELIGIBLE', 'REVIEW_REQUIRED')),
    CONSTRAINT uq_student_role_eligibility UNIQUE (student_id, placement_role_id)
);

CREATE INDEX idx_student_eligibility_student_id ON student_eligibility_results (student_id);
CREATE INDEX idx_student_eligibility_drive_id ON student_eligibility_results (placement_drive_id);
CREATE INDEX idx_student_eligibility_role_id ON student_eligibility_results (placement_role_id);
