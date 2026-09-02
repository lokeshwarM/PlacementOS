-- =============================================================================
-- V12: User Accounts, Authentication Identity & Student Domain Linkage
-- =============================================================================

CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(50) NOT NULL DEFAULT 'STUDENT'
                        CHECK (role IN ('STUDENT', 'ADMIN')),
    student_id      BIGINT UNIQUE REFERENCES students(id) ON DELETE SET NULL,
    profile_status  VARCHAR(50) NOT NULL DEFAULT 'INCOMPLETE'
                        CHECK (profile_status IN ('INCOMPLETE', 'COMPLETE', 'VERIFIED')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_student_id ON users(student_id);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_profile_status ON users(profile_status);
